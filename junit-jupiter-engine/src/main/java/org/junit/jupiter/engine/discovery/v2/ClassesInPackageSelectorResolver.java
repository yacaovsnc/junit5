package org.junit.jupiter.engine.discovery.v2;

import org.junit.platform.engine.discovery.PackageSelector;

import java.util.List;
import java.util.function.Predicate;

import static org.junit.platform.commons.support.ReflectionSupport.findAllClassesInPackage;

public class ClassesInPackageSelectorResolver extends ClasspathScanningSelectorResolver<PackageSelector> {

    public ClassesInPackageSelectorResolver(Predicate<String> classNameFilter, Predicate<Class<?>> classFilter) {
        super(PackageSelector.class, classNameFilter, classFilter);
    }

    @Override
    protected List<Class<?>> findClasses(PackageSelector selector) {
        return findAllClassesInPackage(selector.getPackageName(), this.classFilter, this.classNameFilter);
    }

}
