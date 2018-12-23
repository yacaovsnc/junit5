package org.junit.jupiter.engine.discovery.v2;

import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.discovery.ClassSelector;

public class JavaClassSelectorFilter implements SelectorFilter {

    @Override
    public boolean include(DiscoverySelector selector) {
        if (selector instanceof ClassSelector) {
            return ((ClassSelector) selector).getJavaClass() != null;
        }
        return true;
    }

}
