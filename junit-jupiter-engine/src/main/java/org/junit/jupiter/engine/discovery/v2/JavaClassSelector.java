package org.junit.jupiter.engine.discovery.v2;

import org.junit.platform.engine.DiscoverySelector;

import java.util.Objects;

public class JavaClassSelector implements DiscoverySelector {

    private final Class<?> testClass;

    public JavaClassSelector(Class<?> testClass) {
        this.testClass = testClass;
    }

    public Class<?> getTestClass() {
        return testClass;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JavaClassSelector that = (JavaClassSelector) o;
        return testClass.equals(that.testClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(testClass);
    }
}
