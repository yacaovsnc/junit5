package org.junit.jupiter.engine.discovery.v2;

import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;

public abstract class ClasspathScanningSelectorResolver<T extends DiscoverySelector> extends SingleTypeConvertingSelectorResolver<T> {

    protected final Predicate<String> classNameFilter;
    protected final Predicate<Class<?>> classFilter;

    public ClasspathScanningSelectorResolver(Class<T> selectorClass, Predicate<String> classNameFilter, Predicate<Class<?>> classFilter) {
        super(selectorClass);
        this.classNameFilter = classNameFilter;
        this.classFilter = classFilter;
    }

    @Override
    protected Set<? extends DiscoverySelector> convertTyped(T selector) {
        List<Class<?>> classes = findClasses(selector);
        if (classes.isEmpty()) {
            return emptySet();
        }
        return classes.stream().map(DiscoverySelectors::selectClass).collect(toSet());
    }

    protected abstract List<Class<?>> findClasses(T selector);

}
