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

import static java.util.stream.Collectors.toList;
import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.junit.platform.testkit.ExecutionEvent.byTestDescriptor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apiguardian.api.API;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;

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

	// --- Accessors -----------------------------------------------------------

	public Events events() {
		return new Events(this.testEvents, "Test");
	}

	public Stream<Execution> executions() {
		return this.testExecutions.stream();
	}

	// --- Internals -----------------------------------------------------------

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
