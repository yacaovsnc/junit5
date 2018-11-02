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

import org.apiguardian.api.API;

/**
 * @since 1.4
 */
@API(status = EXPERIMENTAL, since = "1.4")
@FunctionalInterface
public interface EventStatistics {

	void assertStatistic(Events events);

	public static EventStatistics skipped(long expected) {
		return events -> StatisticsUtils.assertStatistic(expected, events.skipped()::stream, "skipped");
	}

	public static EventStatistics started(long expected) {
		return events -> StatisticsUtils.assertStatistic(expected, events.started()::stream, "started");
	}

	public static EventStatistics finished(long expected) {
		return events -> StatisticsUtils.assertStatistic(expected, events.finished()::stream, "finished");
	}

	public static EventStatistics aborted(long expected) {
		return events -> StatisticsUtils.assertStatistic(expected, events.aborted()::stream, "aborted");
	}

	public static EventStatistics succeeded(long expected) {
		return events -> StatisticsUtils.assertStatistic(expected, events.succeeded()::stream, "succeeded");
	}

	public static EventStatistics failed(long expected) {
		return events -> StatisticsUtils.assertStatistic(expected, events.failed()::stream, "failed");
	}

	public static EventStatistics reportingEntryPublished(long expected) {
		return events -> StatisticsUtils.assertStatistic(expected, events.reportingEntryPublished()::stream,
			"reporting entry published");
	}

	public static EventStatistics dynamicallyRegistered(long expected) {
		return events -> StatisticsUtils.assertStatistic(expected, events.dynamicallyRegistered()::stream,
			"dynamically registered");
	}

}
