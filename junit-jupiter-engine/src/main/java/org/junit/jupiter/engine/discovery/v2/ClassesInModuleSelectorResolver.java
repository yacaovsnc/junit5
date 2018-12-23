package org.junit.jupiter.engine.discovery.v2;

import org.junit.platform.engine.discovery.ModuleSelector;

import java.util.List;
import java.util.function.Predicate;

import static org.junit.platform.commons.support.ReflectionSupport.findAllClassesInModule;

public class ClassesInModuleSelectorResolver extends ClasspathScanningSelectorResolver<ModuleSelector> {

    public ClassesInModuleSelectorResolver(Predicate<String> classNameFilter, Predicate<Class<?>> classFilter) {
        super(ModuleSelector.class, classNameFilter, classFilter);
    }

    @Override
    protected List<Class<?>> findClasses(ModuleSelector selector) {
        return findAllClassesInModule(selector.getModuleName(), this.classFilter, this.classNameFilter);
    }

}
