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
import org.junit.jupiter.engine.descriptor.TestFactoryTestDescriptor;
import org.junit.jupiter.engine.discovery.predicates.IsTestFactoryMethod;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;

public class JupiterTestFactoryMethodSelectorResolver extends JupiterMethodSelectorResolver {

	public JupiterTestFactoryMethodSelectorResolver(JupiterConfiguration configuration) {
		super(configuration, new IsTestFactoryMethod(), "test-factory",
			TestFactoryTestDescriptor.DYNAMIC_CONTAINER_SEGMENT_TYPE,
			TestFactoryTestDescriptor.DYNAMIC_TEST_SEGMENT_TYPE);
	}

	@Override
	protected TestDescriptor createTestDescriptor(UniqueId uniqueId, Class<?> testClass, Method method) {
		return new TestFactoryTestDescriptor(uniqueId, testClass, method, configuration);
	}

}
