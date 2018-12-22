package org.junit.jupiter.engine.discovery.v2;

import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.UniqueId;

import java.util.Optional;
import java.util.Set;

public abstract class ConvertingSelectorResolver implements SelectorResolver {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public Optional<Result> resolveSelector(DiscoverySelector selector, Context context) {
        Set<? extends DiscoverySelector> selectors = convert(selector);
        if (selectors.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Result.of(() -> selectors));
    }

    protected abstract Set<? extends DiscoverySelector> convert(DiscoverySelector selector);

    @Override
    public Optional<Result> resolveUniqueId(UniqueId.Segment segment, UniqueId prefix, Context context) {
        return Optional.empty();
    }
}
