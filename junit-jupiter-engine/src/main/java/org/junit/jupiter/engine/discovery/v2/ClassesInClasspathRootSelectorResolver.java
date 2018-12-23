package org.junit.jupiter.engine.discovery.v2;

import org.junit.platform.commons.support.ReflectionSupport;
import org.junit.platform.engine.discovery.ClasspathRootSelector;

import java.util.List;
import java.util.function.Predicate;

public class ClassesInClasspathRootSelectorResolver extends ClasspathScanningSelectorResolver<ClasspathRootSelector> {

    public ClassesInClasspathRootSelectorResolver(Predicate<String> classNameFilter, Predicate<Class<?>> classFilter) {
        super(ClasspathRootSelector.class, classNameFilter, classFilter);
    }

    @Override
    protected List<Class<?>> findClasses(ClasspathRootSelector selector) {
        return ReflectionSupport.findAllClassesInClasspathRoot(selector.getClasspathRoot(), this.classFilter, this.classNameFilter);
    }

}
