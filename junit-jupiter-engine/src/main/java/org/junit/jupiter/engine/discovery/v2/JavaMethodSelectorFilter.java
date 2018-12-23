package org.junit.jupiter.engine.discovery.v2;

import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.discovery.MethodSelector;

public class JavaMethodSelectorFilter implements SelectorFilter {

    @Override
    public boolean include(DiscoverySelector selector) {
        if (selector instanceof MethodSelector) {
            MethodSelector methodSelector = (MethodSelector) selector;
            return methodSelector.getJavaClass() != null && methodSelector.getJavaMethod() != null;
        }
        return true;
    }

}
