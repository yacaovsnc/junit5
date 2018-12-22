package org.junit.jupiter.engine.discovery.v2;

import org.junit.platform.commons.support.ReflectionSupport;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.discovery.ClasspathRootSelector;

import java.util.List;
import java.util.function.Predicate;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.junit.platform.commons.util.BlacklistedExceptions.rethrowIfBlacklisted;

public class ClassesInClasspathRootSelectorResolver extends ClasspathScanningSelectorResolver<ClasspathRootSelector> {

    public ClassesInClasspathRootSelectorResolver(EngineDiscoveryRequest request, Predicate<Class<?>> classFilter) {
        super(ClasspathRootSelector.class, request, classFilter);
    }

    @Override
    protected List<Class<?>> findClasses(ClasspathRootSelector selector) {
        try {
            return ReflectionSupport.findAllClassesInClasspathRoot(selector.getClasspathRoot(), this.classFilter, this.classNameFilter);
        }
        catch (Throwable t) {
            rethrowIfBlacklisted(t);
            logger.debug(t,
                    () -> format("Failed to resolve classes in classpath root '%s'.", selector.getClasspathRoot()));
            return emptyList();
        }
    }

}
