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

import static java.util.stream.Collectors.joining;
import static org.junit.platform.commons.util.BlacklistedExceptions.rethrowIfBlacklisted;
import static org.junit.platform.commons.util.CollectionUtils.getOnlyElement;

import java.util.ArrayList;
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
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.junit.jupiter.engine.discovery.v2.SelectorResolver.Match;
import org.junit.jupiter.engine.discovery.v2.SelectorResolver.Result;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.UniqueIdSelector;
import org.junit.platform.engine.support.filter.ClasspathScanningSupport;

public class EngineDiscoveryRequestResolver {

	private static final Logger logger = LoggerFactory.getLogger(EngineDiscoveryRequestResolver.class);

	private final EngineDiscoveryRequest request;
	private final SelectorResolver.Context defaultContext;
	private final List<SelectorResolver> resolvers;
	private final List<TestDescriptor.Visitor> visitors;
	private final TestDescriptor engineDescriptor;
	private final Map<DiscoverySelector, Result> resolvedSelectors = new LinkedHashMap<>();
	private final Map<UniqueId, Match> resolvedUniqueIds = new LinkedHashMap<>();
	private final Queue<DiscoverySelector> remainingSelectors = new LinkedList<>();
	private final Map<DiscoverySelector, SelectorResolver.Context> contextBySelector = new HashMap<>();

	private EngineDiscoveryRequestResolver(EngineDiscoveryRequest request, TestDescriptor engineDescriptor,
			List<SelectorResolver> resolvers, List<TestDescriptor.Visitor> visitors) {
		this.request = request;
		this.engineDescriptor = engineDescriptor;
		this.resolvers = new ArrayList<>(resolvers);
		this.visitors = new ArrayList<>(visitors);
		this.defaultContext = new DefaultContext(null);
		resolvedUniqueIds.put(engineDescriptor.getUniqueId(), Match.of(engineDescriptor));
	}

	public void resolve() {
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

	private SelectorResolver.Context getContext(DiscoverySelector selector) {
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

	private class DefaultContext implements SelectorResolver.Context {
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
			return resolve(parentSelectorSupplier.get())//
					.map(Result::getMatches).flatMap(matches -> {
						if (matches.size() > 1) {
					// @formatter:off
							String stringRepresentation = matches.stream()
									.map(Match::getTestDescriptor)
									.map(Objects::toString)
									.collect(joining(", "));
							// @formatter:on
							throw new JUnitException(
								"Cannot add descriptor to multiple parents: " + stringRepresentation);
						}
						if (matches.size() == 1) {
							return Optional.of(getOnlyElement(matches).getTestDescriptor());
						}
						return Optional.empty();
					}).flatMap(parent -> createAndAdd(parent, creator));
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

	public static Builder builder(EngineDiscoveryRequest request, TestDescriptor engineDescriptor) {
		return new Builder(request, engineDescriptor);
	}

	public static class Builder {

		private final List<SelectorResolver> resolvers = new LinkedList<>();
		private final List<TestDescriptor.Visitor> visitors = new LinkedList<>();

		private final EngineDiscoveryRequest request;
		private final TestDescriptor engineDescriptor;
		private final Predicate<String> classNameFilter;

		private Builder(EngineDiscoveryRequest request, TestDescriptor engineDescriptor) {
			this.request = request;
			this.engineDescriptor = engineDescriptor;
			this.classNameFilter = ClasspathScanningSupport.buildClassNamePredicate(request);
		}

		public Builder addClassesInClasspathRootSelectorResolver(Predicate<Class<?>> classFilter) {
			return addSelectorResolver(new ClassesInClasspathRootSelectorResolver(classNameFilter, classFilter));
		}

		public Builder addClassesInPackageSelectorResolver(Predicate<Class<?>> classFilter) {
			return addSelectorResolver(new ClassesInPackageSelectorResolver(classNameFilter, classFilter));
		}

		public Builder addClassesInModuleSelectorResolver(Predicate<Class<?>> classFilter) {
			return addSelectorResolver(new ClassesInModuleSelectorResolver(classNameFilter, classFilter));
		}

		public Builder addSelectorResolverWithClassNameFilter(Function<Predicate<String>, SelectorResolver> creator) {
			return addSelectorResolver(creator.apply(classNameFilter));
		}

		public Builder addSelectorResolver(SelectorResolver resolver) {
			resolvers.add(resolver);
			return this;
		}

		public Builder addPruningTestDescriptorVisitor() {
			return addTestDescriptorVisitor(TestDescriptor::prune);
		}

		public Builder addTestDescriptorVisitor(TestDescriptor.Visitor visitor) {
			visitors.add(visitor);
			return this;
		}

		public EngineDiscoveryRequestResolver build() {
			return new EngineDiscoveryRequestResolver(request, engineDescriptor, resolvers, visitors);
		}
	}
}
