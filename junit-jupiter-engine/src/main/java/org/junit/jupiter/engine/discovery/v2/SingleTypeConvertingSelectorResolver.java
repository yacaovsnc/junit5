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

import static java.util.Collections.singleton;

import java.util.Collections;
import java.util.Set;

import org.junit.platform.engine.DiscoverySelector;

public abstract class SingleTypeConvertingSelectorResolver<T extends DiscoverySelector>
		extends ConvertingSelectorResolver {

	private final Class<T> selectorClass;

	public SingleTypeConvertingSelectorResolver(Class<T> selectorClass) {
		this.selectorClass = selectorClass;
	}

	@Override
	public Set<Class<? extends DiscoverySelector>> getSupportedSelectorTypes() {
		return singleton(selectorClass);
	}

	@Override
	protected Set<? extends DiscoverySelector> convert(DiscoverySelector selector) {
		if (selectorClass.isInstance(selector)) {
			return convertTyped(selectorClass.cast(selector));
		}
		return Collections.emptySet();
	}

	protected abstract Set<? extends DiscoverySelector> convertTyped(T selector);

}
