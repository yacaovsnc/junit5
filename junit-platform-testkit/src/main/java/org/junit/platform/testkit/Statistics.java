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
public interface Statistics {

	void assertStatistic(Events events);

	public static Statistics skipped(long expected) {
		return events -> StatisticsUtils.assertStatistic(expected, events.skipped()::stream, "skipped");
	}

	public static Statistics started(long expected) {
		return events -> StatisticsUtils.assertStatistic(expected, events.started()::stream, "started");
	}

	public static Statistics finished(long expected) {
		return events -> StatisticsUtils.assertStatistic(expected, events.finished()::stream, "finished");
	}

	public static Statistics aborted(long expected) {
		return events -> StatisticsUtils.assertStatistic(expected, events.aborted()::stream, "aborted");
	}

	public static Statistics succeeded(long expected) {
		return events -> StatisticsUtils.assertStatistic(expected, events.succeeded()::stream, "succeeded");
	}

	public static Statistics failed(long expected) {
		return events -> StatisticsUtils.assertStatistic(expected, events.failed()::stream, "failed");
	}

	public static Statistics reportingEntryPublished(long expected) {
		return events -> StatisticsUtils.assertStatistic(expected, events.reportingEntryPublished()::stream,
			"reporting entry published");
	}

	public static Statistics dynamicNodeRegistered(long expected) {
		return events -> StatisticsUtils.assertStatistic(expected, events.dynamicNodeRegistered()::stream,
			"dynamic node registered");
	}

}
