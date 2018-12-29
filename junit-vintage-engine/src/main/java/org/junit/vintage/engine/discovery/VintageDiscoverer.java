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

import static org.apiguardian.api.API.Status.INTERNAL;

import org.apiguardian.api.API;
import org.junit.platform.commons.util.ClassFilter;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;
import org.junit.platform.engine.support.discovery.EngineDiscoveryRequestResolver;
import org.junit.vintage.engine.descriptor.RunnerTestDescriptor;

/**
 * @since 4.12
 */
@API(status = INTERNAL, since = "4.12")
public class VintageDiscoverer {

	private static final IsPotentialJUnit4TestClass isPotentialJUnit4TestClass = new IsPotentialJUnit4TestClass();

	public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
		EngineDescriptor engineDescriptor = new EngineDescriptor(uniqueId, "JUnit Vintage");
		RunnerTestDescriptorPostProcessor postProcessor = new RunnerTestDescriptorPostProcessor();
		// @formatter:off
		EngineDiscoveryRequestResolver.configure(discoveryRequest, engineDescriptor)
				.withDefaultsForClassBasedTestEngines(isPotentialJUnit4TestClass)
				.addSelectorResolverWithClassNameFilter(filter -> new ClassSelectorResolver(ClassFilter.of(filter, isPotentialJUnit4TestClass)))
				.addSelectorResolver(new MethodSelectorResolver())
				.resolve();
		engineDescriptor.getChildren().stream()
				.filter(RunnerTestDescriptor.class::isInstance)
				.map(RunnerTestDescriptor.class::cast)
				.forEach(postProcessor::applyFiltersAndCreateDescendants);
		// @formatter:on
		return engineDescriptor;
	}

}
