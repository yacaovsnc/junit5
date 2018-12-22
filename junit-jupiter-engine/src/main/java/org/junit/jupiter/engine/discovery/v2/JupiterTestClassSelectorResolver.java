package org.junit.jupiter.engine.discovery.v2;

import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.jupiter.engine.descriptor.ClassTestDescriptor;
import org.junit.jupiter.engine.descriptor.NestedClassTestDescriptor;
import org.junit.jupiter.engine.discovery.predicates.IsNestedTestClass;
import org.junit.jupiter.engine.discovery.predicates.IsTestClassWithTests;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.engine.discovery.predicates.IsTestClassWithTests.isTestOrTestFactoryOrTestTemplateMethod;
import static org.junit.platform.commons.support.ReflectionSupport.findNestedClasses;
import static org.junit.platform.commons.util.ClassUtils.nullSafeToString;
import static org.junit.platform.commons.util.ReflectionUtils.findMethods;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectUniqueId;

public class JupiterTestClassSelectorResolver implements SelectorResolver {

    private static final Logger logger = LoggerFactory.getLogger(JupiterTestClassSelectorResolver.class);
    private static final IsTestClassWithTests isTestClassWithTests = new IsTestClassWithTests();
    private static final IsNestedTestClass isNestedTestClass = new IsNestedTestClass();

    private final JupiterConfiguration configuration;

    public JupiterTestClassSelectorResolver(JupiterConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public Set<Class<? extends DiscoverySelector>> getSupportedSelectorTypes() {
        return singleton(JavaClassSelector.class);
    }

    @Override
    public Optional<Result> resolveSelector(DiscoverySelector selector, Context context) {
        if (selector instanceof JavaClassSelector) {
            Class<?> testClass = ((JavaClassSelector) selector).getTestClass();
            if (isTestClassWithTests.test(testClass)) {
                return toResult(context.addToEngine(parent -> Optional.of(newClassTestDescriptor(parent, testClass))));
            }
            if (isNestedTestClass.test(testClass)) {
                return toResult(context.addToParentWithSelector(new JavaClassSelector(testClass.getEnclosingClass()),
                        parent -> Optional.of(newNestedClassTestDescriptor(parent, testClass))));
            }
            logger.debug(() -> format("Class '%s' could not be resolved.", nullSafeToString(testClass)));
        }
        return Optional.empty();
    }

    @Override
    public Optional<Result> resolveUniqueId(UniqueId.Segment segment, UniqueId prefix, Context context) {
        if ("class".equals(segment.getType())) {
            String className = segment.getValue();
            return ReflectionUtils.tryToLoadClass(className).toOptional()
                    .filter(isTestClassWithTests)
                    .flatMap(testClass -> toResult(context.addToEngine(parent -> Optional.of(newClassTestDescriptor(parent, testClass)))));
        }
        if ("nested-class".equals(segment.getType())) {
            String simpleClassName = segment.getValue();
            return toResult(context.addToParentWithSelector(selectUniqueId(prefix), parent -> {
                if (parent instanceof ClassTestDescriptor) {
                    String className = ((ClassTestDescriptor) parent).getTestClass().getName() + "$" + simpleClassName;
                    return ReflectionUtils.tryToLoadClass(className).toOptional()
                            .filter(isNestedTestClass)
                            .flatMap(testClass -> Optional.of(newNestedClassTestDescriptor(parent, testClass)));
                }
                return Optional.empty();
            }));
        }
        return Optional.empty();
    }

    private ClassTestDescriptor newClassTestDescriptor(TestDescriptor parent, Class<?> testClass) {
        return new ClassTestDescriptor(parent.getUniqueId().append("class", testClass.getName()), testClass, configuration);
    }

    private NestedClassTestDescriptor newNestedClassTestDescriptor(TestDescriptor parent, Class<?> testClass) {
        return new NestedClassTestDescriptor(parent.getUniqueId().append("nested-class", testClass.getSimpleName()), testClass, configuration);
    }

    private Optional<Result> toResult(Optional<ClassTestDescriptor> testDescriptor) {
        return testDescriptor.map(it -> {
            Class<?> testClass = it.getTestClass();
            // @formatter:off
            return Result.of(it, () -> {
                Stream<JavaMethodSelector> methods = findMethods(testClass, isTestOrTestFactoryOrTestTemplateMethod).stream()
                        .map(method -> new JavaMethodSelector(testClass, method));
                Stream<JavaClassSelector> nestedClasses = findNestedClasses(testClass, isNestedTestClass).stream()
                        .map(JavaClassSelector::new);
                return Stream.concat(methods, nestedClasses).collect(toSet());
            });
            // @formatter:on
        });
    }
}
