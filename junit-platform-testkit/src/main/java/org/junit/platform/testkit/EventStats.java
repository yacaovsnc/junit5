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
import static org.junit.platform.testkit.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.apiguardian.api.API;
import org.junit.platform.testkit.Assertions.Executable;

/**
 * @since 1.4
 */
@API(status = EXPERIMENTAL, since = "1.4")
public class EventStats {

	private final List<Executable> executables = new ArrayList<>();
	private final Events events;

	EventStats(Events events, String category) {
		this.events = events;
	}

	public EventStats skipped(long expected) {
		this.executables.add(() -> assertEquals(expected, this.events.skipped().count(), "skipped"));
		return this;
	}

	public EventStats started(long expected) {
		this.executables.add(() -> assertEquals(expected, this.events.started().count(), "started"));
		return this;
	}

	public EventStats finished(long expected) {
		this.executables.add(() -> assertEquals(expected, this.events.finished().count(), "finished"));
		return this;
	}

	public EventStats aborted(long expected) {
		this.executables.add(() -> assertEquals(expected, this.events.aborted().count(), "aborted"));
		return this;
	}

	public EventStats succeeded(long expected) {
		this.executables.add(() -> assertEquals(expected, this.events.succeeded().count(), "succeeded"));
		return this;
	}

	public EventStats failed(long expected) {
		this.executables.add(() -> assertEquals(expected, this.events.failed().count(), "failed"));
		return this;
	}

	public EventStats reportingEntryPublished(long expected) {
		this.executables.add(
			() -> assertEquals(expected, this.events.reportingEntryPublished().count(), "reporting entry published"));
		return this;
	}

	public EventStats dynamicallyRegistered(long expected) {
		this.executables.add(
			() -> assertEquals(expected, this.events.dynamicallyRegistered().count(), "dynamically registered"));
		return this;
	}

	void assertAll() {
		Assertions.assertAll(this.events.getCategory() + " Event Statistics", this.executables.stream());
	}

}
