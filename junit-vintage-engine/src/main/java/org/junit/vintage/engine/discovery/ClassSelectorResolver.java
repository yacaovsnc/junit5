/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.vintage.engine.discovery;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.junit.vintage.engine.descriptor.VintageTestDescriptor.SEGMENT_TYPE_RUNNER;

import java.util.Optional;
import java.util.Set;

import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.util.ClassFilter;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.UniqueId.Segment;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.support.discovery.SelectorResolver;
import org.junit.runner.Runner;
import org.junit.runners.model.RunnerBuilder;
import org.junit.vintage.engine.descriptor.RunnerTestDescriptor;

/**
 * @since 4.12
 */
class ClassSelectorResolver implements SelectorResolver {

	private static final RunnerBuilder RUNNER_BUILDER = new DefensiveAllDefaultPossibilitiesBuilder();

	private final ClassFilter classFilter;

	ClassSelectorResolver(ClassFilter classFilter) {
		this.classFilter = classFilter;
	}

	@Override
	public Set<Class<? extends DiscoverySelector>> getSupportedSelectorTypes() {
		return singleton(ClassSelector.class);
	}

	@Override
	public Optional<Result> resolveSelector(DiscoverySelector selector, Context context) {
		if (selector instanceof ClassSelector) {
			return resolveTestClass(((ClassSelector) selector).getJavaClass(), context);
		}
		return Optional.empty();
	}

	@Override
	public Optional<Result> resolveUniqueId(UniqueId uniqueId, Context context) {
		Segment lastSegment = uniqueId.getLastSegment();
		if (SEGMENT_TYPE_RUNNER.equals(lastSegment.getType())) {
			String testClassName = lastSegment.getValue();
			Class<?> testClass = ReflectionUtils.tryToLoadClass(testClassName)//
					.getOrThrow(cause -> new JUnitException("Unknown class: " + testClassName, cause));
			return resolveTestClass(testClass, context);
		}
		return Optional.empty();
	}

	private Optional<Result> resolveTestClass(Class<?> testClass, Context context) {
		if (!classFilter.test(testClass)) {
			return Optional.empty();
		}
		Runner runner = RUNNER_BUILDER.safeRunnerForClass(testClass);
		if (runner == null) {
			return Optional.empty();
		}
		return context.addToParent(parent -> Optional.of(createRunnerTestDescriptor(parent, testClass, runner))).map(
			runnerTestDescriptor -> Match.of(runnerTestDescriptor, () -> {
				runnerTestDescriptor.clearFilters();
				return emptySet();
			})).map(Result::of);
	}

	private RunnerTestDescriptor createRunnerTestDescriptor(TestDescriptor parent, Class<?> testClass, Runner runner) {
		UniqueId uniqueId = parent.getUniqueId().append(SEGMENT_TYPE_RUNNER, testClass.getName());
		return new RunnerTestDescriptor(uniqueId, testClass, runner);
	}
}
