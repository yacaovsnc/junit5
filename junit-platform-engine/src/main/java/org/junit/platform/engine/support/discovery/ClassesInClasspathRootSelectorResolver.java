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

import java.util.List;
import java.util.function.Predicate;

import org.junit.platform.commons.support.ReflectionSupport;
import org.junit.platform.engine.discovery.ClasspathRootSelector;

/**
 * @since 1.4
 */
class ClassesInClasspathRootSelectorResolver extends ClasspathScanningSelectorResolver<ClasspathRootSelector> {

	ClassesInClasspathRootSelectorResolver(Predicate<String> classNameFilter, Predicate<Class<?>> classFilter) {
		super(ClasspathRootSelector.class, classNameFilter, classFilter);
	}

	@Override
	protected List<Class<?>> findClasses(ClasspathRootSelector selector) {
		return ReflectionSupport.findAllClassesInClasspathRoot(selector.getClasspathRoot(), this.classFilter,
			this.classNameFilter);
	}

}
