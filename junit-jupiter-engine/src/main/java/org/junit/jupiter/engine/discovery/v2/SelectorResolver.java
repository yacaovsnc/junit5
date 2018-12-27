/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.discovery.v2;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;

public interface SelectorResolver {

	Set<Class<? extends DiscoverySelector>> getSupportedSelectorTypes();

	Optional<Result> resolveSelector(DiscoverySelector selector, Context context);

	Optional<Result> resolveUniqueId(UniqueId uniqueId, Context context);

	interface Context {
		<T extends TestDescriptor> Optional<T> addToEngine(Function<TestDescriptor, Optional<T>> creator);

		<T extends TestDescriptor> Optional<T> addToParentWithSelector(DiscoverySelector selector,
				Function<TestDescriptor, Optional<T>> creator);
	}

	class Result {
		private final TestDescriptor testDescriptor;
		private final Supplier<Set<? extends DiscoverySelector>> additionalSelectorsSupplier;
		private boolean perfectMatch = true;

		public static Result of(TestDescriptor testDescriptor) {
			return new Result(testDescriptor, Collections::emptySet);
		}

		public static Result of(Supplier<Set<? extends DiscoverySelector>> additionalSelectorsSupplier) {
			return new Result(null, additionalSelectorsSupplier);
		}

		public static Result of(TestDescriptor testDescriptor,
				Supplier<Set<? extends DiscoverySelector>> additionalSelectorsSupplier) {
			return new Result(testDescriptor, additionalSelectorsSupplier);
		}

		private Result(TestDescriptor testDescriptor,
				Supplier<Set<? extends DiscoverySelector>> additionalSelectorsSupplier) {
			this.testDescriptor = testDescriptor;
			this.additionalSelectorsSupplier = additionalSelectorsSupplier;
		}

		public boolean isPerfectMatch() {
			return perfectMatch;
		}

		public Result withPerfectMatch() {
			return withPerfectMatch(true);
		}

		public Result withPerfectMatch(boolean perfectMatch) {
			if (this.perfectMatch == perfectMatch) {
				return this;
			}
			Result result = new Result(testDescriptor, additionalSelectorsSupplier);
			result.perfectMatch = perfectMatch;
			return result;
		}

		public Optional<TestDescriptor> getTestDescriptor() {
			return Optional.ofNullable(testDescriptor);
		}

		Set<? extends DiscoverySelector> getAdditionalSelectors() {
			return additionalSelectorsSupplier.get();
		}
	}

}
