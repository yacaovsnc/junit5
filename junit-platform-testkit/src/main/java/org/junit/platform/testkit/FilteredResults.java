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

import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;
import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.junit.platform.testkit.ExecutionEvent.byTestDescriptor;

import java.util.List;
import java.util.function.Predicate;

import org.apiguardian.api.API;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.engine.TestDescriptor;

/**
 * Fluent API for working with filtered {@link ExecutionResults}.
 *
 * @since 1.4
 */
@API(status = EXPERIMENTAL, since = "1.4")
public class FilteredResults {

	private final List<ExecutionEvent> events;
	private final String category;

	FilteredResults(List<ExecutionEvent> events, Predicate<? super TestDescriptor> predicate, String category) {
		Preconditions.notNull(events, "ExecutionEvent list must not be null");
		Preconditions.containsNoNullElements(events, "ExecutionEvent list must not contain null elements");

		this.events = unmodifiableList(extractFilteredEvents(events, predicate));
		this.category = category;
	}

	// --- Accessors -----------------------------------------------------------

	public Events events() {
		return new Events(this.events, this.category);
	}

	public Executions executions() {
		return new Executions(this.events, category);
	}

	// --- Internals -----------------------------------------------------------

	/**
	 * Filter the supplied list of events using the supplied predicate.
	 */
	private static List<ExecutionEvent> extractFilteredEvents(List<ExecutionEvent> events,
			Predicate<? super TestDescriptor> predicate) {

		return events.stream().filter(byTestDescriptor(predicate)).collect(toList());
	}

}
