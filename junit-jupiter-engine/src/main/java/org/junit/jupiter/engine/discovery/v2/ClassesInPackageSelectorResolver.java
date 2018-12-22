package org.junit.jupiter.engine.discovery.v2;

import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.discovery.PackageSelector;

import java.util.List;
import java.util.function.Predicate;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.junit.platform.commons.support.ReflectionSupport.findAllClassesInPackage;
import static org.junit.platform.commons.util.BlacklistedExceptions.rethrowIfBlacklisted;

public class ClassesInPackageSelectorResolver extends ClasspathScanningSelectorResolver<PackageSelector> {

    public ClassesInPackageSelectorResolver(EngineDiscoveryRequest request, Predicate<Class<?>> classFilter) {
        super(PackageSelector.class, request, classFilter);
    }

    @Override
    protected List<Class<?>> findClasses(PackageSelector selector) {
        try {
            return findAllClassesInPackage(selector.getPackageName(), this.classFilter, this.classNameFilter);
        }
        catch (Throwable t) {
            rethrowIfBlacklisted(t);
            logger.debug(t, () -> format("Failed to resolve classes in package '%s'.", selector.getPackageName()));
            return emptyList();
        }
    }

}
