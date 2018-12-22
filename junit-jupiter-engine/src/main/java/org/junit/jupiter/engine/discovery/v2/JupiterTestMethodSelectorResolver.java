package org.junit.jupiter.engine.discovery.v2;

import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor;
import org.junit.jupiter.engine.discovery.predicates.IsTestMethod;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;

import java.lang.reflect.Method;

public class JupiterTestMethodSelectorResolver extends JupiterMethodSelectorResolver {

    public JupiterTestMethodSelectorResolver(JupiterConfiguration configuration) {
        super(configuration, new IsTestMethod(), "method");
    }

    @Override
    protected TestDescriptor createTestDescriptor(UniqueId uniqueId, Class<?> testClass, Method method) {
        return new TestMethodTestDescriptor(uniqueId, testClass, method, configuration);
    }

}
