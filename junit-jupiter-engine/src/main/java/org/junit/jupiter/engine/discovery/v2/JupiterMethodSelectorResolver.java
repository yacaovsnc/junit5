package org.junit.jupiter.engine.discovery.v2;

import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.jupiter.engine.descriptor.ClassTestDescriptor;
import org.junit.jupiter.engine.descriptor.DynamicDescendantFilter;
import org.junit.jupiter.engine.descriptor.Filterable;
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
import java.util.Arrays;
import java.util.LinkedHashSet;
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
    private final Set<String> dynamicDescendantSegmentTypes;

    public JupiterMethodSelectorResolver(JupiterConfiguration configuration, Predicate<Method> methodPredicate, String segmentType, String... dynamicDescendantSegmentTypes) {
        this.configuration = configuration;
        this.methodPredicate = methodPredicate;
        this.segmentType = segmentType;
        this.dynamicDescendantSegmentTypes = new LinkedHashSet<>(Arrays.asList(dynamicDescendantSegmentTypes));
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
    public Optional<Result> resolveUniqueId(UniqueId uniqueId, Context context) {
        Optional<TestDescriptor> testDescriptor = resolveUniqueIdIntoTestDescriptor(uniqueId, context);
        testDescriptor.ifPresent(parent -> {
            if (parent instanceof Filterable) {
                DynamicDescendantFilter filter = ((Filterable) parent).getDynamicDescendantFilter();
                if (uniqueId.equals(parent.getUniqueId())) {
                    filter.allowAll();
                } else {
                    filter.allow(uniqueId);
                }
            }
        });
        return testDescriptor.map(Result::of);
    }

    private Optional<TestDescriptor> resolveUniqueIdIntoTestDescriptor(UniqueId uniqueId, Context context) {
        UniqueId.Segment lastSegment = uniqueId.getLastSegment();
        if (segmentType.equals(lastSegment.getType())) {
            return context.addToParentWithSelector(selectUniqueId(uniqueId.removeLastSegment()), parent -> {
                String methodSpecPart = lastSegment.getValue();
                Class<?> testClass = ((ClassTestDescriptor) parent).getTestClass();
                return Try.call(() -> METHOD_FINDER.findMethod(methodSpecPart, testClass).orElse(null))
                        .ifFailure(exception -> logger.warn(exception, () -> String.format("Unique ID '%s' could not be resolved.", uniqueId)))
                        .toOptional()
                        .filter(methodPredicate)
                        .map(method -> createTestDescriptor(createUniqueId(method, parent), testClass, method));
            });
        }
        if (dynamicDescendantSegmentTypes.contains(lastSegment.getType())) {
            return resolveUniqueIdIntoTestDescriptor(uniqueId.removeLastSegment(), context);
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
