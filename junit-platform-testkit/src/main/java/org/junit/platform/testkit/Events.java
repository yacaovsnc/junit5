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
import static org.junit.platform.testkit.ExecutionEvent.byType;
import static org.junit.platform.testkit.ExecutionEventConditions.assertRecordedExecutionEventsContainsExactly;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apiguardian.api.API;
import org.assertj.core.api.Condition;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestExecutionResult.Status;
import org.junit.platform.testkit.ExecutionEvent.Type;

/**
 * {@link Events} is a facade that provides a fluent API for working with
 * {@linkplain ExecutionEvent execution events}.
 *
 * @since 1.4
 */
@API(status = EXPERIMENTAL, since = "1.4")
public final class Events {

	private final List<ExecutionEvent> events;
	private final String category;

	Events(Stream<ExecutionEvent> events, String category) {
		this(Preconditions.notNull(events, "ExecutionEvent stream must not be null").collect(toList()), category);
	}

	Events(List<ExecutionEvent> events, String category) {
		Preconditions.notNull(events, "ExecutionEvent list must not be null");
		Preconditions.containsNoNullElements(events, "ExecutionEvent list must not contain null elements");

		this.events = Collections.unmodifiableList(events);
		this.category = category;
	}

	// --- Accessors -----------------------------------------------------------

	public List<ExecutionEvent> list() {
		return this.events;
	}

	public Stream<ExecutionEvent> stream() {
		return this.events.stream();
	}

	/**
	 * Shortcut for {@code events.stream().map(mapper)}.
	 */
	public <R> Stream<R> map(Function<? super ExecutionEvent, ? extends R> mapper) {
		return stream().map(mapper);
	}

	public Executions executions() {
		return new Executions(this.events, this.category);
	}

	// --- Statistics ----------------------------------------------------------

	public long count() {
		return this.events.size();
	}

	// --- Built-in Filters ----------------------------------------------------

	public Events skipped() {
		return new Events(eventsByType(Type.SKIPPED), this.category + " Skipped");
	}

	public Events started() {
		return new Events(eventsByType(Type.STARTED), this.category + " Started");
	}

	public Events finished() {
		return new Events(eventsByType(Type.FINISHED), this.category + " Finished");
	}

	public Events aborted() {
		return new Events(finishedEventsByStatus(Status.ABORTED), this.category + " Aborted");
	}

	public Events succeeded() {
		return new Events(finishedEventsByStatus(Status.SUCCESSFUL), this.category + " Successful");
	}

	public Events failed() {
		return new Events(finishedEventsByStatus(Status.FAILED), this.category + " Failed");
	}

	public Events reportingEntryPublished() {
		return new Events(eventsByType(Type.REPORTING_ENTRY_PUBLISHED), this.category + " - Reporting Entry Published");
	}

	public Events dynamicNodeRegistered() {
		return new Events(eventsByType(Type.DYNAMIC_TEST_REGISTERED), this.category + " - Dynamic Test Registered");
	}

	// --- Assertions ----------------------------------------------------------

	@SafeVarargs
	@SuppressWarnings({ "varargs", "unchecked" }) // required for JDK 8
	public final void assertStatistics(Statistics... statistics) {
		assertAll(this.category + " Statistics", Arrays.stream(statistics).map(s -> () -> s.assertStatistic(this)));
	}

	@SafeVarargs
	public final void assertEventsMatchExactly(Condition<? super ExecutionEvent>... conditions) {
		assertRecordedExecutionEventsContainsExactly(this.events, conditions);
	}

	// --- Diagnostics ---------------------------------------------------------

	public void debug() {
		debug(System.out);
	}

	public void debug(PrintStream out) {
		out.println(this.category + " Events:");
		this.events.forEach(event -> out.printf("\t%s%n", event));
	}

	// --- Internals -----------------------------------------------------------

	private Stream<ExecutionEvent> eventsByType(Type type) {
		Preconditions.notNull(type, "Type must not be null");
		return stream().filter(byType(type));
	}

	private Stream<ExecutionEvent> finishedEventsByStatus(Status status) {
		Preconditions.notNull(status, "Status must not be null");
		return eventsByType(Type.FINISHED)//
				.filter(byPayload(TestExecutionResult.class, where(TestExecutionResult::getStatus, isEqual(status))));
	}

}
