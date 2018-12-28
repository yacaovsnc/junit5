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

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

import java.util.Optional;
import java.util.Set;

import org.apiguardian.api.API;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.UniqueId;

/**
 * @since 1.4
 */
@API(status = EXPERIMENTAL, since = "1.4")
public abstract class ConvertingSelectorResolver implements SelectorResolver {

	@Override
	public Optional<Result> resolveSelector(DiscoverySelector selector, Context context) {
		Set<? extends DiscoverySelector> selectors = convert(selector);
		if (selectors.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(Result.of(selectors));
	}

	protected abstract Set<? extends DiscoverySelector> convert(DiscoverySelector selector);

	@Override
	public Optional<Result> resolveUniqueId(UniqueId uniqueId, Context context) {
		return Optional.empty();
	}
}
