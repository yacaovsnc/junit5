package org.junit.jupiter.engine.discovery.v2;

import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.discovery.MethodSelector;

import static java.lang.String.format;
import static org.junit.platform.commons.util.BlacklistedExceptions.rethrowIfBlacklisted;

public class JavaMethodSelectorFilter implements SelectorFilter {

    private final Logger logger = LoggerFactory.getLogger(JavaMethodSelectorFilter.class);

    @Override
    public boolean include(DiscoverySelector selector) {
        if (selector instanceof MethodSelector) {
            MethodSelector methodSelector = (MethodSelector) selector;
            try {
                return methodSelector.getJavaClass() != null && methodSelector.getJavaMethod() != null;
            }
            catch (Throwable t) {
                rethrowIfBlacklisted(t);
                logger.debug(t, () -> format("Method '%s' in class '%s' could not be resolved.", methodSelector.getMethodName(),
                        methodSelector.getClassName()));
                return false;
            }
        }
        return true;
    }

}
