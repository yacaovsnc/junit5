package org.junit.jupiter.engine.discovery.v2;

import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.discovery.ModuleSelector;

import java.util.List;
import java.util.function.Predicate;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.junit.platform.commons.support.ReflectionSupport.findAllClassesInModule;
import static org.junit.platform.commons.util.BlacklistedExceptions.rethrowIfBlacklisted;

public class ClassesInModuleSelectorResolver extends ClasspathScanningSelectorResolver<ModuleSelector> {

    public ClassesInModuleSelectorResolver(EngineDiscoveryRequest request, Predicate<Class<?>> classFilter) {
        super(ModuleSelector.class, request, classFilter);
    }

    @Override
    protected List<Class<?>> findClasses(ModuleSelector selector) {
        try {
            return findAllClassesInModule(selector.getModuleName(), this.classFilter, this.classNameFilter);
        }
        catch (Throwable t) {
            rethrowIfBlacklisted(t);
            logger.debug(t, () -> format("Failed to resolve classes in module '%s'.", selector.getModuleName()));
            return emptyList();
        }
    }

}
