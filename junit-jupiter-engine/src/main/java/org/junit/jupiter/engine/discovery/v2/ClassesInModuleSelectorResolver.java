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

import static org.junit.platform.commons.support.ReflectionSupport.findAllClassesInModule;

import java.util.List;
import java.util.function.Predicate;

import org.junit.platform.engine.discovery.ModuleSelector;

public class ClassesInModuleSelectorResolver extends ClasspathScanningSelectorResolver<ModuleSelector> {

	public ClassesInModuleSelectorResolver(Predicate<String> classNameFilter, Predicate<Class<?>> classFilter) {
		super(ModuleSelector.class, classNameFilter, classFilter);
	}

	@Override
	protected List<Class<?>> findClasses(ModuleSelector selector) {
		return findAllClassesInModule(selector.getModuleName(), this.classFilter, this.classNameFilter);
	}

}
