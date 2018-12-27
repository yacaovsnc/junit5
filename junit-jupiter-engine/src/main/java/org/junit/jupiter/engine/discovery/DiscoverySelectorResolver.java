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

import static org.apiguardian.api.API.Status.INTERNAL;

import org.apiguardian.api.API;
import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.jupiter.engine.discovery.predicates.IsTestClassWithTests;
import org.junit.jupiter.engine.discovery.v2.EngineDiscoveryRequestResolver;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.TestDescriptor;

/**
 * {@code DiscoverySelectorResolver} resolves {@link TestDescriptor TestDescriptors}
 * for containers and tests selected by DiscoverySelectors with the help of the
 * {@code JavaElementsResolver}.
 *
 * <p>This class is the only public entry point into the discovery package.
 *
 * @since 5.0
 */
@API(status = INTERNAL, since = "5.0")
public class DiscoverySelectorResolver {

	private static final IsTestClassWithTests isTestClassWithTests = new IsTestClassWithTests();

	public void resolveSelectors(EngineDiscoveryRequest request, JupiterConfiguration configuration,
			TestDescriptor engineDescriptor) {
		// @formatter:off
		EngineDiscoveryRequestResolver.builder(request, engineDescriptor)
				.addClassesInClasspathRootSelectorResolver(isTestClassWithTests)
				.addClassesInModuleSelectorResolver(isTestClassWithTests)
				.addClassesInPackageSelectorResolver(isTestClassWithTests)
				.addSelectorResolverWithClassNameFilter(filter -> new JupiterTestClassSelectorResolver(filter, configuration))
				.addSelectorResolver(new JupiterTestMethodSelectorResolver(configuration))
				.addSelectorResolver(new JupiterTestFactoryMethodSelectorResolver(configuration))
				.addSelectorResolver(new JupiterTestTemplateMethodSelectorResolver(configuration))
				.addPruningTestDescriptorVisitor()
				.addTestDescriptorVisitor(new MethodOrderingVisitor(configuration))
				.build()
				.resolve();
		// @formatter:on
	}

}
