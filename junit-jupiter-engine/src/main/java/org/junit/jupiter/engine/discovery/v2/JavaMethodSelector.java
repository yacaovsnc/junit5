package org.junit.jupiter.engine.discovery.v2;

import org.junit.platform.engine.DiscoverySelector;

import java.lang.reflect.Method;
import java.util.Objects;

public class JavaMethodSelector implements DiscoverySelector {

    private final Class<?> testClass;
    private final Method method;

    public JavaMethodSelector(Class<?> testClass, Method method) {
        this.testClass = testClass;
        this.method = method;
    }

    public Class<?> getTestClass() {
        return testClass;
    }

    public Method getMethod() {
        return method;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JavaMethodSelector that = (JavaMethodSelector) o;
        return testClass.equals(that.testClass) &&
                method.equals(that.method);
    }

    @Override
    public int hashCode() {
        return Objects.hash(testClass, method);
    }
}
