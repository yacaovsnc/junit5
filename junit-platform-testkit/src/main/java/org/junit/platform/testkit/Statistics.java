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

import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apiguardian.api.API;

/**
 * @since 1.4
 */
@API(status = EXPERIMENTAL, since = "1.4")
@FunctionalInterface
public interface Statistics<T> {

	void assertStatistic(T results);

	public static Statistics<TestResults> skipped(long expected) {
		return results -> assertStatistic(expected, results::skipped, "skipped");
	}

	public static Statistics<TestResults> started(long expected) {
		return results -> assertStatistic(expected, results::started, "started");
	}

	public static Statistics<TestResults> finished(long expected) {
		return results -> assertStatistic(expected, results::finished, "finished");
	}

	public static Statistics<TestResults> aborted(long expected) {
		return results -> assertStatistic(expected, results::aborted, "aborted");
	}

	public static Statistics<TestResults> succeeded(long expected) {
		return results -> assertStatistic(expected, results::succeeded, "succeeded");
	}

	public static Statistics<TestResults> failed(long expected) {
		return results -> assertStatistic(expected, results::failed, "failed");
	}

	private static void assertStatistic(long expected, Supplier<Stream<?>> streamSupplier, String category) {
		Assertions.assertEquals(expected, streamSupplier.get().count(), category);
	}

}
