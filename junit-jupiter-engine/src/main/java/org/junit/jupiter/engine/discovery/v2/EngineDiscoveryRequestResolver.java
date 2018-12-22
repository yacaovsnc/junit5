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

import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.jupiter.engine.discovery.predicates.IsTestClassWithTests;
import org.junit.platform.commons.util.ClassFilter;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.UniqueIdSelector;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class EngineDiscoveryRequestResolver {

	private final EngineDiscoveryRequest request;
	private final SelectorResolver.Context context;
	private final List<SelectorFilter> filters;
	private final List<? extends SelectorResolver> resolvers;
	private final TestDescriptor engineDescriptor;
	private final Map<DiscoverySelector, SelectorResolver.Result> resolvedSelectors = new LinkedHashMap<>();
	private final Map<UniqueId, SelectorResolver.Result> resolvedUniqueIds = new LinkedHashMap<>();
	private final Queue<DiscoverySelector> remainingSelectors = new LinkedList<>();

	public EngineDiscoveryRequestResolver(EngineDiscoveryRequest request, JupiterConfiguration configuration,
										  TestDescriptor engineDescriptor) {
		this.request = request;
		filters = Arrays.asList(
				new JavaClassSelectorFilter(),
				new JavaMethodSelectorFilter()
		);
		resolvers = Arrays.asList(
				new ClassesInClasspathRootSelectorResolver(request, new IsTestClassWithTests()),
				new ClassesInPackageSelectorResolver(request, new IsTestClassWithTests()),
				new ClassesInModuleSelectorResolver(request, new IsTestClassWithTests()),
				new JupiterTestClassSelectorResolver(configuration),
				new JupiterTestMethodSelectorResolver(configuration),
				new JupiterTestTemplateMethodSelectorResolver(configuration),
				new JupiterTestFactoryMethodSelectorResolver(configuration)
		);
		this.engineDescriptor = engineDescriptor;
		this.context = new SelectorResolver.Context() {
			@Override
			public <T extends TestDescriptor> Optional<T> addToParentWithSelector(DiscoverySelector selector, Function<TestDescriptor, Optional<T>> creator) {
				Optional<TestDescriptor> parent = resolve(selector).flatMap(SelectorResolver.Result::getTestDescriptor);
				return createAndAdd(parent, creator);
			}

			@Override
			public <T extends TestDescriptor> Optional<T> addToEngine(Function<TestDescriptor, Optional<T>> creator) {
				return createAndAdd(Optional.of(engineDescriptor), creator);
			}

			private <T extends TestDescriptor> Optional<T> createAndAdd(Optional<TestDescriptor> parent, Function<TestDescriptor, Optional<T>> creator) {
				return parent.flatMap(it -> {
					Optional<T> descriptor = creator.apply(it);
					if (descriptor.isPresent()) {
						UniqueId uniqueId = descriptor.get().getUniqueId();
						if (resolvedUniqueIds.containsKey(uniqueId)) {
							return (Optional<T>) resolvedUniqueIds.get(uniqueId).getTestDescriptor();
						}
					}
					descriptor.ifPresent(it::addChild);
					return descriptor;
				});
			}
		};
		resolvedUniqueIds.put(engineDescriptor.getUniqueId(), SelectorResolver.Result.of(engineDescriptor));
	}

	public void resolve() {
//		ClassFilter classFilter = buildClassFilter(request, isTestClassWithTests);
		doResolve();
//		filter(engineDescriptor, classFilter);
		pruneTree();
	}

	private void doResolve() {
		Set<Class<? extends DiscoverySelector>> selectorTypes = resolvers.stream()
				.map(SelectorResolver::getSupportedSelectorTypes)
				.flatMap(Collection::stream)
				.collect(Collectors.toCollection(LinkedHashSet::new));
		selectorTypes.add(UniqueIdSelector.class);
		// @formatter:off
		selectorTypes.stream()
				.map(request::getSelectorsByType)
				.flatMap(Collection::stream)
				.forEach(remainingSelectors::add);
		// @formatter:on
		while (!remainingSelectors.isEmpty()) {
			DiscoverySelector selector = remainingSelectors.poll();
			if (filters.stream().allMatch(filter -> filter.include(selector))) {
				resolveCompletely(selector);
			}
		}
	}

	private void resolveCompletely(DiscoverySelector selector) {
		resolve(selector).ifPresent(result -> remainingSelectors.addAll(result.getAdditionalSelectors()));
	}

	private Optional<SelectorResolver.Result> resolve(DiscoverySelector selector) {
		if (resolvedSelectors.containsKey(selector)) {
			return Optional.of(resolvedSelectors.get(selector));
		}
		if (selector instanceof UniqueIdSelector) {
			return resolveUniqueId(selector, ((UniqueIdSelector) selector).getUniqueId());
		}
		// @formatter:off
		return resolvers.stream()
				.map(resolver -> resolver.resolveSelector(selector, context))
				.filter(Optional::isPresent)
				.map(Optional::get)
				.findFirst()
				.map(result -> {
					resolvedSelectors.put(selector, result);
					result.getTestDescriptor()
							.ifPresent(testDescriptor -> resolvedUniqueIds.put(testDescriptor.getUniqueId(), result));
					return result;
				});
		// @formatter:on
	}

	private Optional<SelectorResolver.Result> resolveUniqueId(DiscoverySelector selector, UniqueId uniqueId) {
		if (resolvedUniqueIds.containsKey(uniqueId)) {
			return Optional.of(resolvedUniqueIds.get(uniqueId));
		}
		if (!uniqueId.hasPrefix(engineDescriptor.getUniqueId())) {
			return Optional.empty();
		}
		UniqueId.Segment lastSegment = uniqueId.getLastSegment();
		UniqueId remainingUniqueId = uniqueId.removeLastSegment();
		// @formatter:off
		return resolvers.stream()
				.map(resolver -> resolver.resolveUniqueId(lastSegment, remainingUniqueId, context))
				.filter(Optional::isPresent)
				.map(Optional::get)
				.findFirst()
				.map(result -> {
					resolvedSelectors.put(selector, result);
					result.getTestDescriptor()
							.ifPresent(testDescriptor -> resolvedUniqueIds.put(testDescriptor.getUniqueId(), result));
					return result;
				});
		// @formatter:on
	}

	private void filter(TestDescriptor engineDescriptor, ClassFilter classFilter) {
//		new DiscoveryFilterApplier().applyClassNamePredicate(classFilter::match, engineDescriptor);
	}

	private void pruneTree() {
		engineDescriptor.accept(TestDescriptor::prune);
	}

}
