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

import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.junit.platform.testkit.ExecutionEvent.byTestDescriptor;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apiguardian.api.API;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.engine.TestDescriptor;

/**
 * Represents the entirety of multiple test or container execution runs.
 *
 * @since 1.4
 */
@API(status = EXPERIMENTAL, since = "1.4")
public class ExecutionResults {

	private static final String CATEGORY = "All";

	private final List<ExecutionEvent> executionEvents;
	private final Events testEvents;
	private final Events containerEvents;

	/**
	 * Construct an {@link ExecutionResults} given a {@link List} of recorded {@link ExecutionEvent}s.
	 *
	 * @param events the {@link List} of {@link ExecutionEvent}s to use when creating the execution graph, cannot be null
	 */
	ExecutionResults(List<ExecutionEvent> events) {
		Preconditions.notNull(events, "ExecutionEvent list must not be null");
		Preconditions.containsNoNullElements(events, "ExecutionEvent list must not contain null elements");

		this.executionEvents = Collections.unmodifiableList(events);
		this.testEvents = new Events(filterEvents(events, TestDescriptor::isTest), "Test");
		this.containerEvents = new Events(filterEvents(events, TestDescriptor::isContainer), "Contanier");
	}

	// --- Fluent API ----------------------------------------------------------

	/**
	 * Get all recorded events.
	 */
	public Events events() {
		return new Events(this.executionEvents, CATEGORY);
	}

	/**
	 * Get the filtered results for all containers.
	 *
	 * <p>In this context, the word "container" applies to {@link TestDescriptor
	 * TestDescriptors} that return {@code true} from
	 * {@link TestDescriptor#isContainer()}.
	 */
	public Events containers() {
		return this.containerEvents;
	}

	/**
	 * Get the filtered results for all tests.
	 *
	 * <p>In this context, the word "test" applies to {@link TestDescriptor
	 * TestDescriptors} that return {@code true} from
	 * {@link TestDescriptor#isTest()}.
	 */
	public Events tests() {
		return this.testEvents;
	}

	// --- ALL Events ----------------------------------------------------------

	public List<ExecutionEvent> getExecutionEvents() {
		return this.executionEvents;
	}

	// --- Reporting Entry Publication Events ----------------------------------

	public long getReportingEntryPublicationCount() {
		return events().reportingEntryPublished().count();
	}

	// --- Dynamic Registration Events -----------------------------------------

	public long getDynamicTestRegistrationCount() {
		return events().dynamicTestRegistered().count();
	}

	// --- Container Events ----------------------------------------------------

	public long getContainersSkippedCount() {
		return containers().skipped().count();
	}

	public long getContainersStartedCount() {
		return containers().started().count();
	}

	public long getContainersFinishedCount() {
		return containers().finished().count();
	}

	public long getContainersFailedCount() {
		return containers().failed().count();
	}

	public long getContainersAbortedCount() {
		return containers().aborted().count();
	}

	// --- Test Events ---------------------------------------------------------

	public List<ExecutionEvent> getTestsSuccessfulEvents() {
		return tests().succeeded().list();
	}

	public List<ExecutionEvent> getTestsFailedEvents() {
		return tests().failed().list();
	}

	public long getTestsSkippedCount() {
		return tests().skipped().count();
	}

	public long getTestsStartedCount() {
		return tests().started().count();
	}

	public long getTestsFinishedCount() {
		return tests().finished().count();
	}

	public long getTestsSuccessfulCount() {
		return tests().succeeded().count();
	}

	public long getTestsFailedCount() {
		return tests().failed().count();
	}

	public long getTestsAbortedCount() {
		return tests().aborted().count();
	}

	// --- Internals -----------------------------------------------------------

	/**
	 * Filter the supplied list of events using the supplied predicate.
	 */
	private static Stream<ExecutionEvent> filterEvents(List<ExecutionEvent> events,
			Predicate<? super TestDescriptor> predicate) {

		return events.stream().filter(byTestDescriptor(predicate));
	}

}
