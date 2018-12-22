package org.junit.jupiter.engine.discovery.v2;

import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.discovery.MethodSelector;

import java.util.Collections;
import java.util.Set;

import static java.lang.String.format;
import static org.junit.platform.commons.util.BlacklistedExceptions.rethrowIfBlacklisted;

public class JavaMethodConvertingSelectorResolver extends SingleTypeConvertingSelectorResolver<MethodSelector> {

    public JavaMethodConvertingSelectorResolver() {
        super(MethodSelector.class);
    }

    @Override
    protected Set<? extends DiscoverySelector> convertTyped(MethodSelector selector) {
        try {
            return Collections.singleton(new JavaMethodSelector(selector.getJavaClass(), selector.getJavaMethod()));
        }
        catch (Throwable t) {
            rethrowIfBlacklisted(t);
            logger.debug(t, () -> format("Method '%s' in class '%s' could not be resolved.", selector.getMethodName(),
                    selector.getClassName()));
            return Collections.emptySet();
        }
    }

}
