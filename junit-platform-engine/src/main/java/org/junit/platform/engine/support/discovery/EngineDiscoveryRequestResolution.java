/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.support.discovery;

import static java.util.stream.Collectors.joining;
import static org.junit.platform.commons.util.BlacklistedExceptions.rethrowIfBlacklisted;
import static org.junit.platform.commons.util.CollectionUtils.getOnlyElement;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.UniqueIdSelector;
import org.junit.platform.engine.support.discovery.SelectorResolver.Context;
import org.junit.platform.engine.support.discovery.SelectorResolver.Match;
import org.junit.platform.engine.support.discovery.SelectorResolver.Result;

class EngineDiscoveryRequestResolution {

	private final Logger logger;
	private final EngineDiscoveryRequest request;
	private final Context defaultContext;
	private final List<SelectorResolver> resolvers;
	private final List<TestDescriptor.Visitor> visitors;
	private final TestDescriptor engineDescriptor;
	private final Map<DiscoverySelector, Result> resolvedSelectors = new LinkedHashMap<>();
	private final Map<UniqueId, Match> resolvedUniqueIds = new LinkedHashMap<>();
	private final Queue<DiscoverySelector> remainingSelectors = new LinkedList<>();
	private final Map<DiscoverySelector, Context> contextBySelector = new HashMap<>();

	EngineDiscoveryRequestResolution(Logger logger, EngineDiscoveryRequest request, TestDescriptor engineDescriptor,
			List<SelectorResolver> resolvers, List<TestDescriptor.Visitor> visitors) {
		this.logger = logger;
		this.request = request;
		this.engineDescriptor = engineDescriptor;
		this.resolvers = resolvers;
		this.visitors = visitors;
		this.defaultContext = new DefaultContext(null);
		this.resolvedUniqueIds.put(engineDescriptor.getUniqueId(), Match.of(engineDescriptor));
	}

	void run() {
		// @formatter:off
		getSupportedSelectorTypes().stream()
				.map(request::getSelectorsByType)
				.flatMap(Collection::stream)
				.forEach(remainingSelectors::add);
		// @formatter:on
		while (!remainingSelectors.isEmpty()) {
			resolveCompletely(remainingSelectors.poll());
		}
		visitors.forEach(engineDescriptor::accept);
	}

	private Set<Class<? extends DiscoverySelector>> getSupportedSelectorTypes() {
		// @formatter:off
		Set<Class<? extends DiscoverySelector>> selectorTypes = resolvers.stream()
				.map(SelectorResolver::getSupportedSelectorTypes)
				.flatMap(Collection::stream)
				.collect(Collectors.toCollection(LinkedHashSet::new));
		// @formatter:on
		selectorTypes.add(UniqueIdSelector.class);
		return selectorTypes;
	}

	private void resolveCompletely(DiscoverySelector selector) {
		try {
			Optional<Result> result = resolve(selector);
			if (result.isPresent()) {
				enqueueAdditionalSelectors(result.get());
			}
			else {
				logUnresolvedSelector(selector, null);
			}
		}
		catch (Throwable t) {
			rethrowIfBlacklisted(t);
			logUnresolvedSelector(selector, t);
		}
	}

	private void enqueueAdditionalSelectors(Result result) {
		Set<? extends DiscoverySelector> additionalSelectors = result.getAdditionalSelectors();
		remainingSelectors.addAll(additionalSelectors);
		if (result.isPerfectMatch()) {
			result.getMatches().forEach(match -> {
				Set<? extends DiscoverySelector> childSelectors = match.getChildSelectors();
				if (!childSelectors.isEmpty()) {
					remainingSelectors.addAll(childSelectors);
					DefaultContext context = new DefaultContext(match.getTestDescriptor());
					childSelectors.forEach(selector -> contextBySelector.put(selector, context));
				}
			});
		}
	}

	private Optional<Result> resolve(DiscoverySelector selector) {
		if (resolvedSelectors.containsKey(selector)) {
			return Optional.of(resolvedSelectors.get(selector));
		}
		if (selector instanceof UniqueIdSelector) {
			return resolveUniqueId(selector, ((UniqueIdSelector) selector).getUniqueId());
		}
		return resolve(selector, resolver -> resolver.resolveSelector(selector, getContext(selector)));
	}

	private Optional<Result> resolveUniqueId(DiscoverySelector selector, UniqueId uniqueId) {
		if (resolvedUniqueIds.containsKey(uniqueId)) {
			return Optional.of(Result.of(resolvedUniqueIds.get(uniqueId)));
		}
		if (!uniqueId.hasPrefix(engineDescriptor.getUniqueId())) {
			return Optional.empty();
		}
		return resolve(selector, resolver -> resolver.resolveUniqueId(uniqueId, getContext(selector)));
	}

	private Context getContext(DiscoverySelector selector) {
		return contextBySelector.getOrDefault(selector, defaultContext);
	}

	private Optional<Result> resolve(DiscoverySelector selector,
			Function<SelectorResolver, Optional<Result>> resolutionFunction) {
		// @formatter:off
		return resolvers.stream()
				.map(resolutionFunction)
				.filter(Optional::isPresent)
				.map(Optional::get)
				.findFirst()
				.map(result -> {
					contextBySelector.remove(selector);
					resolvedSelectors.put(selector, result);
					result.getMatches()
							.forEach(match -> resolvedUniqueIds.put(match.getTestDescriptor().getUniqueId(), match));
					return result;
				});
		// @formatter:on
	}

	private void logUnresolvedSelector(DiscoverySelector selector, Throwable cause) {
		BiConsumer<Throwable, Supplier<String>> loggingConsumer = logger::debug;
		if (selector instanceof UniqueIdSelector) {
			UniqueId uniqueId = ((UniqueIdSelector) selector).getUniqueId();
			if (uniqueId.hasPrefix(engineDescriptor.getUniqueId())) {
				loggingConsumer = logger::warn;
			}
			else {
				return;
			}
		}
		loggingConsumer.accept(cause, () -> selector + " could not be resolved.");
	}

	private class DefaultContext implements Context {
		private final TestDescriptor parent;

		DefaultContext(TestDescriptor parent) {
			this.parent = parent;
		}

		@Override
		public <T extends TestDescriptor> Optional<T> addToParent(Function<TestDescriptor, Optional<T>> creator) {
			if (parent != null) {
				return createAndAdd(parent, creator);
			}
			return createAndAdd(engineDescriptor, creator);
		}

		@Override
		public <T extends TestDescriptor> Optional<T> addToParent(Supplier<DiscoverySelector> parentSelectorSupplier,
				Function<TestDescriptor, Optional<T>> creator) {
			if (parent != null) {
				return createAndAdd(parent, creator);
			}
			return resolve(parentSelectorSupplier.get()).flatMap(parent -> createAndAdd(parent, creator));
		}

		@Override
		public Optional<TestDescriptor> resolve(DiscoverySelector selector) {
			// @formatter:off
			return EngineDiscoveryRequestResolution.this.resolve(selector)
					.map(Result::getMatches)
					.flatMap(matches -> {
						if (matches.size() > 1) {
							String stringRepresentation = matches.stream()
									.map(Match::getTestDescriptor)
									.map(Objects::toString)
									.collect(joining(", "));
							throw new JUnitException(
								"Selector " + selector + " did not yield unique test descriptor: " + stringRepresentation);
						}
						if (matches.size() == 1) {
							return Optional.of(getOnlyElement(matches).getTestDescriptor());
						}
						return Optional.empty();
					});
			// @formatter:on
		}

		@SuppressWarnings("unchecked")
		private <T extends TestDescriptor> Optional<T> createAndAdd(TestDescriptor parent,
				Function<TestDescriptor, Optional<T>> creator) {
			Optional<T> child = creator.apply(parent);
			if (child.isPresent()) {
				UniqueId uniqueId = child.get().getUniqueId();
				if (resolvedUniqueIds.containsKey(uniqueId)) {
					return Optional.of((T) resolvedUniqueIds.get(uniqueId).getTestDescriptor());
				}
			}
			child.ifPresent(parent::addChild);
			return child;
		}
	}

}
