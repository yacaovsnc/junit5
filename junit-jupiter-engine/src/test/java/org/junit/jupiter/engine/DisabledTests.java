/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.platform.testkit.Statistics.aborted;
import static org.junit.platform.testkit.Statistics.failed;
import static org.junit.platform.testkit.Statistics.finished;
import static org.junit.platform.testkit.Statistics.skipped;
import static org.junit.platform.testkit.Statistics.started;
import static org.junit.platform.testkit.Statistics.succeeded;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.platform.testkit.Events;
import org.junit.platform.testkit.ExecutionResults;

/**
 * Integration tests that verify support for {@link Disabled @Disabled} in the {@link JupiterTestEngine}.
 *
 * @since 5.0
 */
class DisabledTests extends AbstractJupiterTestEngineTests {

	@Test
	void executeTestsWithDisabledTestClass() {
		ExecutionResults results = executeTestsForClass(DisabledTestClassTestCase.class);

		results.containers().events().assertStatistics(skipped(1));
		results.tests().events().assertStatistics(started(0));
	}

	@Test
	void executeTestsWithDisabledTestMethods() throws Exception {
		Events testEvents = executeTestsForClass(DisabledTestMethodsTestCase.class).tests().events();

		// MANUAL APPROACH for asserting statistics
		//
		// @formatter:off
		assertAll(
			() -> assertEquals(1, testEvents.started().count(), "# tests started"),
			() -> assertEquals(1, testEvents.skipped().count(), "# tests skipped"),
			() -> assertEquals(1, testEvents.finished().count(), "# tests finished"),
			() -> assertEquals(0, testEvents.aborted().count(), "# tests aborted"),
			() -> assertEquals(1, testEvents.succeeded().count(), "# tests succeeded"),
			() -> assertEquals(0, testEvents.failed().count(), "# tests failed")
		);
		// @formatter:on

		// BUILT-IN APPROACH for asserting statistics
		//
		// @formatter:off
		testEvents.assertStatistics(
			skipped(1),
			started(1),
			finished(1),
			aborted(0),
			succeeded(1),
			failed(0)
		);
		// @formatter:on

		String method = DisabledTestMethodsTestCase.class.getDeclaredMethod("disabledTest").toString();
		String reason = testEvents.skipped().map(e -> e.getPayloadAs(String.class)).findFirst().orElse(null);
		assertEquals(method + " is @Disabled", reason);
	}

	// -------------------------------------------------------------------

	@Disabled
	static class DisabledTestClassTestCase {

		@Test
		void disabledTest() {
			fail("this should be @Disabled");
		}
	}

	static class DisabledTestMethodsTestCase {

		@Test
		void enabledTest() {
		}

		@Test
		@Disabled
		void disabledTest() {
			fail("this should be @Disabled");
		}

	}

}
