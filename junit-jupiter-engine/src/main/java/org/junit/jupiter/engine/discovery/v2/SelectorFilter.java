package org.junit.jupiter.engine.discovery.v2;

import org.junit.platform.engine.DiscoverySelector;

public interface SelectorFilter {

    boolean include(DiscoverySelector selector);

}
