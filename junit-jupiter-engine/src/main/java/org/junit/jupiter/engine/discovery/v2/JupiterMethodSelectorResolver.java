package org.junit.jupiter.engine.discovery.v2;

import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.jupiter.engine.descriptor.ClassTestDescriptor;
import org.junit.jupiter.engine.discovery.MethodFinder;
import org.junit.platform.commons.function.Try;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.platform.commons.util.ClassUtils;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.MethodSelector;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import static java.util.Collections.singleton;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectUniqueId;

public abstract class JupiterMethodSelectorResolver implements SelectorResolver {

    private static final MethodFinder METHOD_FINDER = new MethodFinder();

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final JupiterConfiguration configuration;
    private final Predicate<Method> methodPredicate;
    private final String segmentType;

    public JupiterMethodSelectorResolver(JupiterConfiguration configuration, Predicate<Method> methodPredicate, String segmentType) {
        this.configuration = configuration;
        this.methodPredicate = methodPredicate;
        this.segmentType = segmentType;
    }

    @Override
    public Set<Class<? extends DiscoverySelector>> getSupportedSelectorTypes() {
        return singleton(MethodSelector.class);
    }

    @Override
    public Optional<Result> resolveSelector(DiscoverySelector selector, Context context) {
        if (selector instanceof MethodSelector) {
            return resolveMethodSelector((MethodSelector) selector, context);
        }
        return Optional.empty();
    }

    private Optional<Result> resolveMethodSelector(MethodSelector selector, Context resolver) {
        Method method = selector.getJavaMethod();
        if (methodPredicate.test(method)) {
            Class<?> testClass = selector.getJavaClass();
            return resolver.addToParentWithSelector(selectClass(testClass), parent ->
                    Optional.of(createTestDescriptor(createUniqueId(method, parent), testClass, method)))
                    .map(Result::of);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Result> resolveUniqueId(UniqueId.Segment segment, UniqueId prefix, Context context) {
        if (segmentType.equals(segment.getType())) {
            return context.addToParentWithSelector(selectUniqueId(prefix), parent -> {
                String methodSpecPart = segment.getValue();
                Class<?> testClass = ((ClassTestDescriptor) parent).getTestClass();
                return Try.call(() -> METHOD_FINDER.findMethod(methodSpecPart, testClass).orElse(null))
                        .ifFailure(exception -> logger.warn(exception, () -> String.format("Unique ID '%s' could not be resolved.", prefix.append(segment))))
                        .toOptional()
                        .filter(methodPredicate)
                        .map(method -> createTestDescriptor(createUniqueId(method, parent), testClass, method));
            }).map(Result::of);
        }
        return Optional.empty();
    }

    protected abstract TestDescriptor createTestDescriptor(UniqueId uniqueId, Class<?> testClass, Method method);

    private UniqueId createUniqueId(Method method, TestDescriptor parent) {
        String methodId = String.format("%s(%s)", method.getName(),
                ClassUtils.nullSafeToString(method.getParameterTypes()));
        return parent.getUniqueId().append(segmentType, methodId);
    }
}
