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

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;

/**
 * @since 1.4
 */
abstract class ClasspathScanningSelectorResolver<T extends DiscoverySelector>
		extends SingleTypeConvertingSelectorResolver<T> {

	final Predicate<String> classNameFilter;
	final Predicate<Class<?>> classFilter;

	ClasspathScanningSelectorResolver(Class<T> selectorClass, Predicate<String> classNameFilter,
			Predicate<Class<?>> classFilter) {
		super(selectorClass);
		this.classNameFilter = Preconditions.notNull(classNameFilter, "classNameFilter must not be null");
		this.classFilter = Preconditions.notNull(classFilter, "classFilter must not be null");
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

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		ClasspathScanningSelectorResolver<?> that = (ClasspathScanningSelectorResolver<?>) o;
		return classNameFilter.equals(that.classNameFilter) && classFilter.equals(that.classFilter);
	}

	@Override
	public int hashCode() {
		return Objects.hash(classNameFilter, classFilter);
	}
}
