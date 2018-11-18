/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params;

import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @since 5.4
 * @see <a href="https://github.com/junit-team/junit5/issues/1665">gh-1665</a>
 * @see ParameterizedTestIntegrationTests
 */
@Disabled("Only intended to be executed for manual inspection")
@TestInstance(Lifecycle.PER_CLASS)
class Issue1665Tests {

	@ParameterizedTest
	@MethodSource
	void streamOfOneDimensionalPrimitiveArrays(int[] par) {
		System.out.println(Arrays.toString(par));
	}

	@ParameterizedTest
	@MethodSource
	void streamOfTwoDimensionalPrimitiveArrays(int[][] par) {
		System.out.println(Arrays.deepToString(par));
	}

	@ParameterizedTest
	@MethodSource
	void streamOfTwoDimensionalPrimitiveArraysWrappedInObjectArrays(int[][] par) {
		System.out.println(Arrays.deepToString(par));
	}

	@ParameterizedTest
	@MethodSource
	void streamOfTwoDimensionalPrimitiveArraysWrappedInArguments(int[][] par) {
		System.out.println(Arrays.deepToString(par));
	}

	Stream<int[]> streamOfOneDimensionalPrimitiveArrays() {
		return Stream.of(new int[] { 1 }, new int[] { 2 });
	}

	Stream<int[][]> streamOfTwoDimensionalPrimitiveArrays() {
		return Stream.of(new int[][] { { 1, 2 } }, new int[][] { { 3, 4 } });
	}

	Stream<Object[]> streamOfTwoDimensionalPrimitiveArraysWrappedInObjectArrays() {
		return Stream.of(new Object[] { new int[][] { { 1, 2 } } }, new Object[] { new int[][] { { 3, 4 } } });
	}

	Stream<Arguments> streamOfTwoDimensionalPrimitiveArraysWrappedInArguments() {
		return Stream.of(arguments((Object) new int[][] { { 1, 2 } }), arguments((Object) new int[][] { { 3, 4 } }));
	}

}
