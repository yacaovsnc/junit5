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
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.ToStringBuilder;
import org.junit.platform.engine.TestExecutionResult;

/**
 * {@code TerminationInfo} is a union type that allows propagation of terminated
 * container/test state, supporting either the <em>reason</em> if the container/test
 * was skipped or the {@link TestExecutionResult} if the container/test was executed.
 *
 * @since 1.4
 * @see Execution#getTerminationInfo()
 */
@API(status = EXPERIMENTAL, since = "1.4")
public class TerminationInfo {

	public static TerminationInfo skipped(String skipReason) {
		return new TerminationInfo(true, skipReason, null);
	}

	public static TerminationInfo executed(TestExecutionResult executionResult) {
		return new TerminationInfo(false, null, executionResult);
	}

	private final boolean skipped;
	private final String skipReason;
	private final TestExecutionResult testExecutionResult;

	private TerminationInfo(boolean skipped, String skipReason, TestExecutionResult testExecutionResult) {
		boolean executed = (testExecutionResult != null);
		Preconditions.condition((skipped ^ executed),
			"TerminationInfo must represent either a skipped execution or a TestExecutionResult but not both");

		this.skipped = skipped;
		this.skipReason = skipReason;
		this.testExecutionResult = testExecutionResult;
	}

	public boolean skipped() {
		return this.skipped;
	}

	public boolean notSkipped() {
		return !skipped();
	}

	public boolean executed() {
		return (this.testExecutionResult != null);
	}

	public String getSkipReason() {
		if (skipped()) {
			return this.skipReason;
		}
		// else
		throw new UnsupportedOperationException("No skip reason contained in this TerminationInfo");
	}

	public TestExecutionResult getExecutionResult() {
		if (executed()) {
			return this.testExecutionResult;
		}
		// else
		throw new UnsupportedOperationException("No TestExecutionResult contained in this TerminationInfo");
	}

	@Override
	public String toString() {
		ToStringBuilder builder = new ToStringBuilder(this);
		if (skipped()) {
			builder.append("skipped", skipped()).append("reason", this.skipReason);
		}
		else {
			builder.append("executed", executed()).append("result", this.testExecutionResult);
		}
		return builder.toString();
	}

}
