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

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.engine.discovery.predicates.IsTestClassWithTests.isTestOrTestFactoryOrTestTemplateMethod;
import static org.junit.platform.commons.support.ReflectionSupport.findNestedClasses;
import static org.junit.platform.commons.util.ReflectionUtils.findMethods;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectUniqueId;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.jupiter.engine.descriptor.ClassTestDescriptor;
import org.junit.jupiter.engine.descriptor.NestedClassTestDescriptor;
import org.junit.jupiter.engine.discovery.predicates.IsNestedTestClass;
import org.junit.jupiter.engine.discovery.predicates.IsTestClassWithTests;
import org.junit.jupiter.engine.discovery.v2.SelectorResolver;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.discovery.MethodSelector;

public class JupiterTestClassSelectorResolver implements SelectorResolver {

	private static final IsTestClassWithTests isTestClassWithTests = new IsTestClassWithTests();
	private static final IsNestedTestClass isNestedTestClass = new IsNestedTestClass();

	private final Predicate<String> classNameFilter;
	private final JupiterConfiguration configuration;

	public JupiterTestClassSelectorResolver(Predicate<String> classNameFilter, JupiterConfiguration configuration) {
		this.classNameFilter = classNameFilter;
		this.configuration = configuration;
	}

	@Override
	public Set<Class<? extends DiscoverySelector>> getSupportedSelectorTypes() {
		return singleton(ClassSelector.class);
	}

	@Override
	public Optional<Result> resolveSelector(DiscoverySelector selector, Context context) {
		if (selector instanceof ClassSelector) {
			Class<?> testClass = ((ClassSelector) selector).getJavaClass();
			if (isTestClassWithTests.test(testClass)) {
				// Nested tests are never filtered out
				if (classNameFilter.test(testClass.getName())) {
					return toResult(
						context.addToEngine(parent -> Optional.of(newClassTestDescriptor(parent, testClass))));
				}
			}
			else if (isNestedTestClass.test(testClass)) {
				return toResult(context.addToParentWithSelector(selectClass(testClass.getEnclosingClass()),
					parent -> Optional.of(newNestedClassTestDescriptor(parent, testClass))));
			}
		}
		return Optional.empty();
	}

	@Override
	public Optional<Result> resolveUniqueId(UniqueId uniqueId, Context context) {
		UniqueId.Segment lastSegment = uniqueId.getLastSegment();
		if ("class".equals(lastSegment.getType())) {
			String className = lastSegment.getValue();
			return ReflectionUtils.tryToLoadClass(className).toOptional().filter(isTestClassWithTests).flatMap(
				testClass -> toResult(
					context.addToEngine(parent -> Optional.of(newClassTestDescriptor(parent, testClass)))));
		}
		if ("nested-class".equals(lastSegment.getType())) {
			String simpleClassName = lastSegment.getValue();
			return toResult(context.addToParentWithSelector(selectUniqueId(uniqueId.removeLastSegment()), parent -> {
				if (parent instanceof ClassTestDescriptor) {
					String className = ((ClassTestDescriptor) parent).getTestClass().getName() + "$" + simpleClassName;
					return ReflectionUtils.tryToLoadClass(className).toOptional().filter(isNestedTestClass).flatMap(
						testClass -> Optional.of(newNestedClassTestDescriptor(parent, testClass)));
				}
				return Optional.empty();
			}));
		}
		return Optional.empty();
	}

	private ClassTestDescriptor newClassTestDescriptor(TestDescriptor parent, Class<?> testClass) {
		return new ClassTestDescriptor(parent.getUniqueId().append("class", testClass.getName()), testClass,
			configuration);
	}

	private NestedClassTestDescriptor newNestedClassTestDescriptor(TestDescriptor parent, Class<?> testClass) {
		return new NestedClassTestDescriptor(parent.getUniqueId().append("nested-class", testClass.getSimpleName()),
			testClass, configuration);
	}

	private Optional<Result> toResult(Optional<ClassTestDescriptor> testDescriptor) {
		return testDescriptor.map(it -> {
			Class<?> testClass = it.getTestClass();
			// @formatter:off
            return Result.of(it, () -> {
                Stream<MethodSelector> methods = findMethods(testClass, isTestOrTestFactoryOrTestTemplateMethod).stream()
                        .map(method -> selectMethod(testClass, method));
                Stream<ClassSelector> nestedClasses = findNestedClasses(testClass, isNestedTestClass).stream()
                        .map(DiscoverySelectors::selectClass);
                return Stream.concat(methods, nestedClasses).collect(toSet());
            });
            // @formatter:on
		});
	}
}
