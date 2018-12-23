package org.junit.jupiter.engine.discovery.v2;

import org.junit.platform.engine.DiscoverySelector;

import java.util.Collections;
import java.util.Set;

import static java.util.Collections.singleton;

public abstract class SingleTypeConvertingSelectorResolver<T extends DiscoverySelector> extends ConvertingSelectorResolver {

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
