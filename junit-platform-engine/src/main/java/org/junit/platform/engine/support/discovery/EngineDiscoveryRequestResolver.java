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

import static java.util.stream.Collectors.toCollection;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.support.filter.ClasspathScanningSupport;

public class EngineDiscoveryRequestResolver<T extends TestDescriptor> {

	private static final Logger logger = LoggerFactory.getLogger(EngineDiscoveryRequestResolver.class);

	private final List<Function<InitializationContext<T>, SelectorResolver>> resolverCreators;
	private final List<Function<InitializationContext<T>, TestDescriptor.Visitor>> visitorCreators;

	private EngineDiscoveryRequestResolver(List<Function<InitializationContext<T>, SelectorResolver>> resolverCreators,
			List<Function<InitializationContext<T>, TestDescriptor.Visitor>> visitorCreators) {
		this.resolverCreators = new ArrayList<>(resolverCreators);
		this.visitorCreators = new ArrayList<>(visitorCreators);
	}

	public void resolve(EngineDiscoveryRequest request, T engineDescriptor) {
		InitializationContext<T> initializationContext = new DefaultInitializationContext<>(request, engineDescriptor);
		List<SelectorResolver> resolvers = instantiate(resolverCreators, initializationContext);
		List<TestDescriptor.Visitor> visitors = instantiate(visitorCreators, initializationContext);
		new EngineDiscoveryRequestResolution(logger, request, engineDescriptor, resolvers, visitors).run();
	}

	private <R> List<R> instantiate(List<Function<InitializationContext<T>, R>> creators,
			InitializationContext<T> context) {
		return creators.stream().map(creator -> creator.apply(context)).collect(toCollection(ArrayList::new));
	}

	public static <T extends TestDescriptor> Builder<T> builder() {
		return new Builder<>();
	}

	public static class Builder<T extends TestDescriptor> {

		private final List<Function<InitializationContext<T>, SelectorResolver>> resolverCreators = new ArrayList<>();
		private final List<Function<InitializationContext<T>, TestDescriptor.Visitor>> visitorCreators = new ArrayList<>();

		private Builder() {
		}

		public Builder<T> withDefaultsForClassBasedTestEngines(Predicate<Class<?>> classFilter) {
			return addClassesInClasspathRootSelectorResolver(classFilter)//
					.addClassesInModuleSelectorResolver(classFilter)//
					.addClassesInPackageSelectorResolver(classFilter);
		}

		public Builder<T> addClassesInClasspathRootSelectorResolver(Predicate<Class<?>> classFilter) {
			return addSelectorResolver(
				context -> new ClassesInClasspathRootSelectorResolver(context.getClassNameFilter(), classFilter));
		}

		public Builder<T> addClassesInPackageSelectorResolver(Predicate<Class<?>> classFilter) {
			return addSelectorResolver(
				context -> new ClassesInPackageSelectorResolver(context.getClassNameFilter(), classFilter));
		}

		public Builder<T> addClassesInModuleSelectorResolver(Predicate<Class<?>> classFilter) {
			return addSelectorResolver(
				context -> new ClassesInModuleSelectorResolver(context.getClassNameFilter(), classFilter));
		}

		public Builder<T> addSelectorResolver(SelectorResolver resolver) {
			return addSelectorResolver(context -> resolver);
		}

		public Builder<T> addSelectorResolver(Function<InitializationContext<T>, SelectorResolver> resolverCreator) {
			resolverCreators.add(resolverCreator);
			return this;
		}

		public Builder<T> addTestDescriptorVisitor(
				Function<InitializationContext<T>, TestDescriptor.Visitor> visitorCreator) {
			visitorCreators.add(visitorCreator);
			return this;
		}

		public EngineDiscoveryRequestResolver<T> build() {
			return new EngineDiscoveryRequestResolver<>(resolverCreators, visitorCreators);
		}
	}

	public interface InitializationContext<T extends TestDescriptor> {
		EngineDiscoveryRequest getDiscoveryRequest();

		T getEngineDescriptor();

		Predicate<String> getClassNameFilter();
	}

	private static class DefaultInitializationContext<T extends TestDescriptor> implements InitializationContext<T> {

		private final EngineDiscoveryRequest request;
		private final T engineDescriptor;
		private final Predicate<String> classNameFilter;

		DefaultInitializationContext(EngineDiscoveryRequest request, T engineDescriptor) {
			this.request = request;
			this.engineDescriptor = engineDescriptor;
			this.classNameFilter = ClasspathScanningSupport.buildClassNamePredicate(request);
		}

		@Override
		public EngineDiscoveryRequest getDiscoveryRequest() {
			return request;
		}

		@Override
		public T getEngineDescriptor() {
			return engineDescriptor;
		}

		@Override
		public Predicate<String> getClassNameFilter() {
			return classNameFilter;
		}
	}

}
