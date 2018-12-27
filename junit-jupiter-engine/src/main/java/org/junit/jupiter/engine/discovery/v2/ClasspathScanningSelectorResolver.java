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
import static java.util.stream.Collectors.toSet;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;

/**
 * @since 5.4
 */
abstract class ClasspathScanningSelectorResolver<T extends DiscoverySelector>
		extends SingleTypeConvertingSelectorResolver<T> {

	final Predicate<String> classNameFilter;
	final Predicate<Class<?>> classFilter;

	ClasspathScanningSelectorResolver(Class<T> selectorClass, Predicate<String> classNameFilter,
			Predicate<Class<?>> classFilter) {
		super(selectorClass);
		this.classNameFilter = classNameFilter;
		this.classFilter = classFilter;
	}

	@Override
	protected Set<? extends DiscoverySelector> convertTyped(T selector) {
		List<Class<?>> classes = findClasses(selector);
		if (classes.isEmpty()) {
			return emptySet();
		}
		return classes.stream().map(DiscoverySelectors::selectClass).collect(toSet());
	}

	protected abstract List<Class<?>> findClasses(T selector);

}
