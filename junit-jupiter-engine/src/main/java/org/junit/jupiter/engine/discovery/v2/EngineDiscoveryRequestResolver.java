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

import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.UniqueIdSelector;
import org.junit.platform.engine.support.filter.ClasspathScanningSupport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.junit.platform.commons.util.BlacklistedExceptions.rethrowIfBlacklisted;

public class EngineDiscoveryRequestResolver {

	private static final Logger logger = LoggerFactory.getLogger(EngineDiscoveryRequestResolver.class);

	private final EngineDiscoveryRequest request;
	private final SelectorResolver.Context defaultContext;
	private final List<SelectorResolver> resolvers;
	private final List<TestDescriptor.Visitor> visitors;
	private final TestDescriptor engineDescriptor;
	private final Map<DiscoverySelector, SelectorResolver.Result> resolvedSelectors = new LinkedHashMap<>();
	private final Map<UniqueId, SelectorResolver.Result> resolvedUniqueIds = new LinkedHashMap<>();
	private final Queue<DiscoverySelector> remainingSelectors = new LinkedList<>();
	private final Map<DiscoverySelector, SelectorResolver.Context> contextBySelector = new HashMap<>();

	private EngineDiscoveryRequestResolver(EngineDiscoveryRequest request, TestDescriptor engineDescriptor,
			List<SelectorResolver> resolvers, List<TestDescriptor.Visitor> visitors) {
		this.request = request;
		this.engineDescriptor = engineDescriptor;
		this.resolvers = new ArrayList<>(resolvers);
		this.visitors = new ArrayList<>(visitors);
		this.defaultContext = new DefaultContext(null);
		resolvedUniqueIds.put(engineDescriptor.getUniqueId(), SelectorResolver.Result.of(engineDescriptor));
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
			Optional<SelectorResolver.Result> result = resolve(selector);
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

	private void enqueueAdditionalSelectors(SelectorResolver.Result result) {
		if (result.isPerfectMatch()) {
			Set<? extends DiscoverySelector> additionalSelectors = result.getAdditionalSelectors();
			remainingSelectors.addAll(additionalSelectors);
			result.getTestDescriptor().map(DefaultContext::new)//
					.ifPresent(
						context -> additionalSelectors.forEach(selector -> contextBySelector.put(selector, context)));
		}
	}

	private Optional<SelectorResolver.Result> resolve(DiscoverySelector selector) {
		if (resolvedSelectors.containsKey(selector)) {
			return Optional.of(resolvedSelectors.get(selector));
		}
		if (selector instanceof UniqueIdSelector) {
			return resolveUniqueId(selector, ((UniqueIdSelector) selector).getUniqueId());
		}
		return resolve(selector, resolver -> resolver.resolveSelector(selector, getContext(selector)));
	}

	private Optional<SelectorResolver.Result> resolveUniqueId(DiscoverySelector selector, UniqueId uniqueId) {
		if (resolvedUniqueIds.containsKey(uniqueId)) {
			return Optional.of(resolvedUniqueIds.get(uniqueId));
		}
		if (!uniqueId.hasPrefix(engineDescriptor.getUniqueId())) {
			return Optional.empty();
		}
		return resolve(selector, resolver -> resolver.resolveUniqueId(uniqueId, getContext(selector)));
	}

	private SelectorResolver.Context getContext(DiscoverySelector selector) {
		return contextBySelector.getOrDefault(selector, defaultContext);
	}

	private Optional<SelectorResolver.Result> resolve(DiscoverySelector selector,
			Function<SelectorResolver, Optional<SelectorResolver.Result>> resolutionFunction) {
		// @formatter:off
		return resolvers.stream()
				.map(resolutionFunction)
				.filter(Optional::isPresent)
				.map(Optional::get)
				.findFirst()
				.map(result -> {
					contextBySelector.remove(selector);
					resolvedSelectors.put(selector, result);
					result.getTestDescriptor()
							.ifPresent(testDescriptor -> resolvedUniqueIds.put(testDescriptor.getUniqueId(), result.withPerfectMatch()));
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
			return resolve(parentSelectorSupplier.get()).flatMap(SelectorResolver.Result::getTestDescriptor)//
					.flatMap(parent -> createAndAdd(parent, creator));
		}

		@SuppressWarnings("unchecked")
		private <T extends TestDescriptor> Optional<T> createAndAdd(TestDescriptor parent,
				Function<TestDescriptor, Optional<T>> creator) {
			Optional<T> child = creator.apply(parent);
			if (child.isPresent()) {
				UniqueId uniqueId = child.get().getUniqueId();
				if (resolvedUniqueIds.containsKey(uniqueId)) {
					return (Optional<T>) resolvedUniqueIds.get(uniqueId).getTestDescriptor();
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
