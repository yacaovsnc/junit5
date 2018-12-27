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

import java.lang.reflect.Method;

import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor;
import org.junit.jupiter.engine.discovery.predicates.IsTestMethod;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;

public class JupiterTestMethodSelectorResolver extends JupiterMethodSelectorResolver {

	public JupiterTestMethodSelectorResolver(JupiterConfiguration configuration) {
		super(configuration, new IsTestMethod(), "method");
	}

	@Override
	protected TestDescriptor createTestDescriptor(UniqueId uniqueId, Class<?> testClass, Method method) {
		return new TestMethodTestDescriptor(uniqueId, testClass, method, configuration);
	}

}
