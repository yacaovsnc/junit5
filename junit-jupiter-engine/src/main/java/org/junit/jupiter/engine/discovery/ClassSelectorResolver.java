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
import static java.util.function.Predicate.isEqual;
import static java.util.stream.Collectors.toCollection;
import static org.junit.jupiter.engine.discovery.predicates.IsTestClassWithTests.isTestOrTestFactoryOrTestTemplateMethod;
import static org.junit.platform.commons.support.ReflectionSupport.findNestedClasses;
import static org.junit.platform.commons.util.FunctionUtils.where;
import static org.junit.platform.commons.util.ReflectionUtils.findMethods;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectUniqueId;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.jupiter.engine.descriptor.ClassTestDescriptor;
import org.junit.jupiter.engine.descriptor.NestedClassTestDescriptor;
import org.junit.jupiter.engine.discovery.predicates.IsNestedTestClass;
import org.junit.jupiter.engine.discovery.predicates.IsTestClassWithTests;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.discovery.MethodSelector;
import org.junit.platform.engine.support.discovery.SelectorResolver;

class ClassSelectorResolver implements SelectorResolver {

	private static final IsTestClassWithTests isTestClassWithTests = new IsTestClassWithTests();
	private static final IsNestedTestClass isNestedTestClass = new IsNestedTestClass();

	private final Predicate<String> classNameFilter;
	private final JupiterConfiguration configuration;

	ClassSelectorResolver(Predicate<String> classNameFilter, JupiterConfiguration configuration) {
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
						context.addToParent(parent -> Optional.of(newClassTestDescriptor(parent, testClass))));
				}
			}
			else if (isNestedTestClass.test(testClass)) {
				return toResult(context.addToParent(() -> selectClass(testClass.getEnclosingClass()),
					parent -> Optional.of(newNestedClassTestDescriptor(parent, testClass))));
			}
		}
		return Optional.empty();
	}

	@Override
	public Optional<Result> resolveUniqueId(UniqueId uniqueId, Context context) {
		UniqueId.Segment lastSegment = uniqueId.getLastSegment();
		if (ClassTestDescriptor.SEGMENT_TYPE.equals(lastSegment.getType())) {
			String className = lastSegment.getValue();
			return ReflectionUtils.tryToLoadClass(className).toOptional().filter(isTestClassWithTests).flatMap(
				testClass -> toResult(
					context.addToParent(parent -> Optional.of(newClassTestDescriptor(parent, testClass)))));
		}
		if (NestedClassTestDescriptor.SEGMENT_TYPE.equals(lastSegment.getType())) {
			String simpleClassName = lastSegment.getValue();
			return toResult(context.addToParent(() -> selectUniqueId(uniqueId.removeLastSegment()), parent -> {
				if (parent instanceof ClassTestDescriptor) {
					Class<?> parentTestClass = ((ClassTestDescriptor) parent).getTestClass();
					// TODO add test for resolving unique id of inherited nested test class
					return ReflectionUtils.findNestedClasses(parentTestClass,
						isNestedTestClass.and(
							where(Class::getSimpleName, isEqual(simpleClassName)))).stream().findFirst().flatMap(
								testClass -> Optional.of(newNestedClassTestDescriptor(parent, testClass)));
				}
				return Optional.empty();
			}));
		}
		return Optional.empty();
	}

	private ClassTestDescriptor newClassTestDescriptor(TestDescriptor parent, Class<?> testClass) {
		return new ClassTestDescriptor(
			parent.getUniqueId().append(ClassTestDescriptor.SEGMENT_TYPE, testClass.getName()), testClass,
			configuration);
	}

	private NestedClassTestDescriptor newNestedClassTestDescriptor(TestDescriptor parent, Class<?> testClass) {
		return new NestedClassTestDescriptor(
			parent.getUniqueId().append(NestedClassTestDescriptor.SEGMENT_TYPE, testClass.getSimpleName()), testClass,
			configuration);
	}

	private Optional<Result> toResult(Optional<ClassTestDescriptor> testDescriptor) {
		return testDescriptor.map(it -> {
			Class<?> testClass = it.getTestClass();
			// @formatter:off
            return Result.of(Match.of(it, () -> {
                Stream<MethodSelector> methods = findMethods(testClass, isTestOrTestFactoryOrTestTemplateMethod).stream()
                        .map(method -> selectMethod(testClass, method));
				Stream<ClassSelector> nestedClasses = findNestedClasses(testClass, isNestedTestClass).stream()
						.map(DiscoverySelectors::selectClass);
				return Stream.concat(methods, nestedClasses).collect(toCollection((Supplier<Set<DiscoverySelector>>) LinkedHashSet::new));
            }));
            // @formatter:on
		});
	}
}
