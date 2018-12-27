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

import static org.junit.platform.commons.support.ReflectionSupport.findAllClassesInPackage;

import java.util.List;
import java.util.function.Predicate;

import org.junit.platform.engine.discovery.PackageSelector;

public class ClassesInPackageSelectorResolver extends ClasspathScanningSelectorResolver<PackageSelector> {

	public ClassesInPackageSelectorResolver(Predicate<String> classNameFilter, Predicate<Class<?>> classFilter) {
		super(PackageSelector.class, classNameFilter, classFilter);
	}

	@Override
	protected List<Class<?>> findClasses(PackageSelector selector) {
		return findAllClassesInPackage(selector.getPackageName(), this.classFilter, this.classNameFilter);
	}

}
