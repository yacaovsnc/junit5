package org.junit.jupiter.engine.discovery.v2;

import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.discovery.ClassSelector;

import static java.lang.String.format;
import static org.junit.platform.commons.util.BlacklistedExceptions.rethrowIfBlacklisted;

public class JavaClassSelectorFilter implements SelectorFilter {

    private final Logger logger = LoggerFactory.getLogger(JavaClassSelectorFilter.class);

    @Override
    public boolean include(DiscoverySelector selector) {
        if (selector instanceof ClassSelector) {
            ClassSelector classSelector = (ClassSelector) selector;
            try {
                return classSelector.getJavaClass() != null;
            }
            catch (Throwable t) {
                rethrowIfBlacklisted(t);
                logger.debug(t, () -> format("Class '%s' could not be resolved.", classSelector.getClassName()));
                return false;
            }
        }
        return true;
    }

}
