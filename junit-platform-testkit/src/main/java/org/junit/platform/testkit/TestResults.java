/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.testkit;

import static java.util.function.Predicate.isEqual;
import static java.util.stream.Collectors.toList;
import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.junit.platform.commons.util.FunctionUtils.where;
import static org.junit.platform.testkit.Assertions.assertAll;
import static org.junit.platform.testkit.ExecutionEvent.byPayload;
import static org.junit.platform.testkit.ExecutionEvent.byTestDescriptor;
import static org.junit.platform.testkit.ExecutionEvent.byType;
import static org.junit.platform.testkit.ExecutionEventConditions.assertRecordedExecutionEventsContainsExactly;

import java.io.PrintStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apiguardian.api.API;
import org.assertj.core.api.Condition;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestExecutionResult.Status;
import org.junit.platform.testkit.ExecutionEvent.Type;

/**
 * Fluent API for test results.
 *
 * <p>In this context, the word "test" applies only to {@link TestDescriptor
 * TestDescriptors} of type {@link TestDescriptor.Type#TEST TEST}.
 *
 * @since 1.4
 */
@API(status = EXPERIMENTAL, since = "1.4")
public final class TestResults {

	private final List<ExecutionEvent> testEvents;
	private final List<Execution> testExecutions;

	TestResults(List<ExecutionEvent> events) {
		Preconditions.notNull(events, "ExecutionEvent list must not be null");
		Preconditions.containsNoNullElements(events, "ExecutionEvent list must not contain null elements");

		this.testEvents = extractTestEvents(events);
		this.testExecutions = createExecutions(this.testEvents);
	}

	// --- Executions ----------------------------------------------------------

	public Stream<Execution> executions() {
		return this.testExecutions.stream();
	}

	// --- Events --------------------------------------------------------------

	public Stream<ExecutionEvent> events() {
		return this.testEvents.stream();
	}

	public Stream<ExecutionEvent> skipped() {
		return eventsByType(Type.SKIPPED);
	}

	public Stream<ExecutionEvent> started() {
		return eventsByType(Type.STARTED);
	}

	public Stream<ExecutionEvent> finished() {
		return eventsByType(Type.FINISHED);
	}

	public Stream<ExecutionEvent> aborted() {
		return finishedEvents(Status.ABORTED);
	}

	public Stream<ExecutionEvent> succeeded() {
		return finishedEvents(Status.SUCCESSFUL);
	}

	public Stream<ExecutionEvent> failed() {
		return finishedEvents(Status.FAILED);
	}

	@SafeVarargs
	public final void assertStatistics(Statistics<TestResults>... statistics) {
		assertAll("Test Statistics", Arrays.stream(statistics).map(s -> () -> s.assertStatistic(this)));
	}

	@SafeVarargs
	public final void assertEventsMatchExactly(Condition<? super ExecutionEvent>... conditions) {
		assertRecordedExecutionEventsContainsExactly(events().collect(toList()), conditions);
	}

	public void debugEvents() {
		debugEvents(System.out);
	}

	public void debugEvents(PrintStream out) {
		out.println("Test Events:");
		events().forEach(event -> out.printf("\t%s%n", event));
	}

	// --- Internals -----------------------------------------------------------

	private Stream<ExecutionEvent> eventsByType(Type type) {
		Preconditions.notNull(type, "Type must not be null");
		return events().filter(byType(type));
	}

	private Stream<ExecutionEvent> finishedEvents(Status status) {
		Preconditions.notNull(status, "Status must not be null");
		return eventsByType(Type.FINISHED)//
				.filter(byPayload(TestExecutionResult.class, where(TestExecutionResult::getStatus, isEqual(status))));
	}

	// -------------------------------------------------------------------------

	/**
	 * Cache test events by extracting them from the full list of events.
	 */
	private static List<ExecutionEvent> extractTestEvents(List<ExecutionEvent> events) {
		return Collections.unmodifiableList(events.stream() //
				.filter(byTestDescriptor(TestDescriptor::isTest)) //
				.collect(toList()));
	}

	/**
	 * Cache test executions by reading from the full list of test events.
	 */
	private static List<Execution> createExecutions(List<ExecutionEvent> executionEvents) {
		List<Execution> executions = new ArrayList<>();
		Map<TestDescriptor, Instant> executionStarts = new HashMap<>();

		for (ExecutionEvent executionEvent : executionEvents) {
			switch (executionEvent.getType()) {
				case STARTED: {
					executionStarts.put(executionEvent.getTestDescriptor(), executionEvent.getTimestamp());
					break;
				}
				case SKIPPED: {
					Instant startInstant = executionStarts.get(executionEvent.getTestDescriptor());
					Execution skippedEvent = Execution.skipped(executionEvent.getTestDescriptor(),
						startInstant != null ? startInstant : executionEvent.getTimestamp(),
						executionEvent.getTimestamp(), executionEvent.getPayloadAs(String.class));
					executions.add(skippedEvent);
					executionStarts.remove(executionEvent.getTestDescriptor());
					break;
				}
				case FINISHED: {
					Execution finishedEvent = Execution.finished(executionEvent.getTestDescriptor(),
						executionStarts.get(executionEvent.getTestDescriptor()), executionEvent.getTimestamp(),
						executionEvent.getPayloadAs(TestExecutionResult.class));
					executions.add(finishedEvent);
					executionStarts.remove(executionEvent.getTestDescriptor());
					break;
				}
				default: {
					// Ignore reporting entry publication and dynamic test registration events
					break;
				}
			}
		}

		return Collections.unmodifiableList(executions);
	}

}
