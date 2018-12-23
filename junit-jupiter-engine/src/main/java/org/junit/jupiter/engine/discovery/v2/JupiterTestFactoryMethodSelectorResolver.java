package org.junit.jupiter.engine.discovery.v2;

import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.jupiter.engine.descriptor.TestFactoryTestDescriptor;
import org.junit.jupiter.engine.discovery.predicates.IsTestFactoryMethod;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;

import java.lang.reflect.Method;

public class JupiterTestFactoryMethodSelectorResolver extends JupiterMethodSelectorResolver {

    public JupiterTestFactoryMethodSelectorResolver(JupiterConfiguration configuration) {
        super(configuration, new IsTestFactoryMethod(), "test-factory", TestFactoryTestDescriptor.DYNAMIC_CONTAINER_SEGMENT_TYPE, TestFactoryTestDescriptor.DYNAMIC_TEST_SEGMENT_TYPE);
    }

    @Override
    protected TestDescriptor createTestDescriptor(UniqueId uniqueId, Class<?> testClass, Method method) {
        return new TestFactoryTestDescriptor(uniqueId, testClass, method, configuration);
    }

}
