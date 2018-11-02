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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apiguardian.api.API;
import org.junit.platform.testkit.Assertions.Executable;

/**
 * @since 1.4
 */
@API(status = EXPERIMENTAL, since = "1.4")
public class EventStats {

	private final Events events;
	private final String category;

	private final List<Executable> executables = new ArrayList<>();

	EventStats(Events events, String category) {
		this.events = events;
		this.category = category;
	}

	public EventStats skipped(long expected) {
		this.executables.add(() -> assertStatistic(expected, this.events.skipped()::stream, "skipped"));
		return this;
	}

	public EventStats started(long expected) {
		this.executables.add(() -> assertStatistic(expected, events.started()::stream, "started"));
		return this;
	}

	public EventStats finished(long expected) {
		this.executables.add(() -> assertStatistic(expected, events.finished()::stream, "finished"));
		return this;
	}

	public EventStats aborted(long expected) {
		this.executables.add(() -> assertStatistic(expected, events.aborted()::stream, "aborted"));
		return this;
	}

	public EventStats succeeded(long expected) {
		this.executables.add(() -> assertStatistic(expected, events.succeeded()::stream, "succeeded"));
		return this;
	}

	public EventStats failed(long expected) {
		this.executables.add(() -> assertStatistic(expected, events.failed()::stream, "failed"));
		return this;
	}

	public EventStats reportingEntryPublished(long expected) {
		this.executables.add(
			() -> assertStatistic(expected, events.reportingEntryPublished()::stream, "reporting entry published"));
		return this;
	}

	public EventStats dynamicallyRegistered(long expected) {
		this.executables.add(
			() -> assertStatistic(expected, events.dynamicallyRegistered()::stream, "dynamically registered"));
		return this;
	}

	void assertAll() {
		Assertions.assertAll(this.category + " Event Statistics", this.executables.stream());
	}

	private static void assertStatistic(long expected, Supplier<Stream<?>> streamSupplier, String category) {
		Assertions.assertEquals(expected, streamSupplier.get().count(), category);
	}

}
