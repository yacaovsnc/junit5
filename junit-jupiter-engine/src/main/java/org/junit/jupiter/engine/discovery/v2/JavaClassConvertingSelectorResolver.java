package org.junit.jupiter.engine.discovery.v2;

import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.discovery.ClassSelector;

import java.util.Collections;
import java.util.Set;

import static java.lang.String.format;
import static org.junit.platform.commons.util.BlacklistedExceptions.rethrowIfBlacklisted;

public class JavaClassConvertingSelectorResolver extends SingleTypeConvertingSelectorResolver<ClassSelector> {

    public JavaClassConvertingSelectorResolver() {
        super(ClassSelector.class);
    }

    @Override
    protected Set<? extends DiscoverySelector> convertTyped(ClassSelector selector) {
        try {
            return Collections.singleton(new JavaClassSelector(selector.getJavaClass()));
        }
        catch (Throwable t) {
            rethrowIfBlacklisted(t);
            logger.debug(t, () -> format("Class '%s' could not be resolved.", selector.getClassName()));
            return Collections.emptySet();
        }
    }

}
