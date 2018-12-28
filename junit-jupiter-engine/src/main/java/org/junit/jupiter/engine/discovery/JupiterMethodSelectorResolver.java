/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.discovery;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectUniqueId;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.jupiter.engine.descriptor.ClassTestDescriptor;
import org.junit.jupiter.engine.descriptor.Filterable;
import org.junit.jupiter.engine.discovery.v2.SelectorResolver;
import org.junit.platform.commons.util.ClassUtils;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.MethodSelector;

public abstract class JupiterMethodSelectorResolver implements SelectorResolver {

	private static final MethodFinder METHOD_FINDER = new MethodFinder();

	protected final JupiterConfiguration configuration;
	private final Predicate<Method> methodPredicate;
	private final String segmentType;
	private final Set<String> dynamicDescendantSegmentTypes;

	public JupiterMethodSelectorResolver(JupiterConfiguration configuration, Predicate<Method> methodPredicate,
			String segmentType, String... dynamicDescendantSegmentTypes) {
		this.configuration = configuration;
		this.methodPredicate = methodPredicate;
		this.segmentType = segmentType;
		this.dynamicDescendantSegmentTypes = new LinkedHashSet<>(Arrays.asList(dynamicDescendantSegmentTypes));
	}

	@Override
	public Set<Class<? extends DiscoverySelector>> getSupportedSelectorTypes() {
		return singleton(MethodSelector.class);
	}

	@Override
	public Optional<Result> resolveSelector(DiscoverySelector selector, Context context) {
		if (selector instanceof MethodSelector) {
			return resolveMethodSelector((MethodSelector) selector, context);
		}
		return Optional.empty();
	}

	private Optional<Result> resolveMethodSelector(MethodSelector selector, Context resolver) {
		Method method = selector.getJavaMethod();
		if (methodPredicate.test(method)) {
			Class<?> testClass = selector.getJavaClass();
			return resolver.addToParent(() -> selectClass(testClass),
				parent -> Optional.of(createTestDescriptor(createUniqueId(method, parent), testClass, method))).map(
					this::toResult);
		}
		return Optional.empty();
	}

	@Override
	public Optional<Result> resolveUniqueId(UniqueId uniqueId, Context context) {
		return resolveUniqueIdIntoTestDescriptor(uniqueId, context).map(testDescriptor -> {
			boolean perfectMatch = uniqueId.equals(testDescriptor.getUniqueId());
			if (testDescriptor instanceof Filterable) {
				Filterable filterable = (Filterable) testDescriptor;
				if (perfectMatch) {
					filterable.getDynamicDescendantFilter().allowAll();
				}
				else {
					filterable.getDynamicDescendantFilter().allow(uniqueId);
				}
			}
			return toResult(testDescriptor).withPerfectMatch(perfectMatch);
		});
	}

	private Result toResult(TestDescriptor testDescriptor) {
		return Result.of(Match.of(testDescriptor, () -> {
			if (testDescriptor instanceof Filterable) {
				((Filterable) testDescriptor).getDynamicDescendantFilter().allowAll();
			}
			return emptySet();
		}));
	}

	private Optional<TestDescriptor> resolveUniqueIdIntoTestDescriptor(UniqueId uniqueId, Context context) {
		UniqueId.Segment lastSegment = uniqueId.getLastSegment();
		if (segmentType.equals(lastSegment.getType())) {
			return context.addToParent(() -> selectUniqueId(uniqueId.removeLastSegment()), parent -> {
				String methodSpecPart = lastSegment.getValue();
				Class<?> testClass = ((ClassTestDescriptor) parent).getTestClass();
				// @formatter:off
                return METHOD_FINDER.findMethod(methodSpecPart, testClass)
                        .filter(methodPredicate)
                        .map(method -> createTestDescriptor(createUniqueId(method, parent), testClass, method));
                // @formatter:on
			});
		}
		if (dynamicDescendantSegmentTypes.contains(lastSegment.getType())) {
			return resolveUniqueIdIntoTestDescriptor(uniqueId.removeLastSegment(), context);
		}
		return Optional.empty();
	}

	protected abstract TestDescriptor createTestDescriptor(UniqueId uniqueId, Class<?> testClass, Method method);

	private UniqueId createUniqueId(Method method, TestDescriptor parent) {
		String methodId = String.format("%s(%s)", method.getName(),
			ClassUtils.nullSafeToString(method.getParameterTypes()));
		return parent.getUniqueId().append(segmentType, methodId);
	}
}
