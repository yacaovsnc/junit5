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

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.unmodifiableSet;
import static org.apiguardian.api.API.Status.EXPERIMENTAL;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apiguardian.api.API;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;

/**
 * @since 5.4
 */
@API(status = EXPERIMENTAL, since = "5.4")
public interface SelectorResolver {

	Set<Class<? extends DiscoverySelector>> getSupportedSelectorTypes();

	Optional<Result> resolveSelector(DiscoverySelector selector, Context context);

	Optional<Result> resolveUniqueId(UniqueId uniqueId, Context context);

	/**
	 * @since 5.4
	 */
	@API(status = EXPERIMENTAL, since = "5.4")
	interface Context {
		<T extends TestDescriptor> Optional<T> addToParent(Function<TestDescriptor, Optional<T>> creator);

		<T extends TestDescriptor> Optional<T> addToParent(Supplier<DiscoverySelector> parentSelectorSupplier,
				Function<TestDescriptor, Optional<T>> creator);
	}

	/**
	 * @since 5.4
	 */
	@API(status = EXPERIMENTAL, since = "5.4")
	class Result {
		private final Set<Match> matches;
		private final Set<? extends DiscoverySelector> additionalSelectors;
		private boolean perfectMatch = true;

		public static Result of(Match match) {
			return new Result(singleton(match), emptySet());
		}

		public static Result of(Collection<Match> matches) {
			return of(matches, emptySet());
		}

		public static Result of(Set<? extends DiscoverySelector> additionalSelectors) {
			return new Result(emptySet(), additionalSelectors);
		}

		public static Result of(Collection<Match> matches, Set<? extends DiscoverySelector> additionalSelectors) {
			return new Result(unmodifiableSet(new LinkedHashSet<>(matches)), additionalSelectors);
		}

		private Result(Set<Match> matches, Set<? extends DiscoverySelector> additionalSelectors) {
			// TODO validate not both empty
			this.matches = matches;
			this.additionalSelectors = additionalSelectors;
		}

		public boolean isPerfectMatch() {
			return perfectMatch;
		}

		public Result withPerfectMatch(boolean perfectMatch) {
			if (this.perfectMatch == perfectMatch) {
				return this;
			}
			Result result = new Result(matches, additionalSelectors);
			result.perfectMatch = perfectMatch;
			return result;
		}

		public Set<Match> getMatches() {
			return matches;
		}

		public Set<? extends DiscoverySelector> getAdditionalSelectors() {
			return additionalSelectors;
		}
	}

	/**
	 * @since 5.4
	 */
	@API(status = EXPERIMENTAL, since = "5.4")
	class Match {
		private final TestDescriptor testDescriptor;
		private final Supplier<Set<? extends DiscoverySelector>> childSelectorsSupplier;

		public static Match of(TestDescriptor testDescriptor) {
			return new Match(testDescriptor, Collections::emptySet);
		}

		public static Match of(TestDescriptor testDescriptor,
				Supplier<Set<? extends DiscoverySelector>> childSelectorsSupplier) {
			return new Match(testDescriptor, childSelectorsSupplier);
		}

		private Match(TestDescriptor testDescriptor,
				Supplier<Set<? extends DiscoverySelector>> childSelectorsSupplier) {
			this.testDescriptor = testDescriptor;
			this.childSelectorsSupplier = childSelectorsSupplier;
		}

		public TestDescriptor getTestDescriptor() {
			return testDescriptor;
		}

		public Set<? extends DiscoverySelector> getChildSelectors() {
			return childSelectorsSupplier.get();
		}
	}

}
