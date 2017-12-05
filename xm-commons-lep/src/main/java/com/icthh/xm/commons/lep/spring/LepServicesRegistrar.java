package com.icthh.xm.commons.lep.spring;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The {@link LepServicesRegistrar} register LEP service bean definitions for interfaces marked with {@link LepService}
 * when processing @{@link Configuration} classes with annottaion {@link EnableLepServices}.
 */
public class LepServicesRegistrar implements ImportBeanDefinitionRegistrar,
    ResourceLoaderAware, EnvironmentAware { // BeanClassLoaderAware

    /**
     * Resource loader for {@link LepService} annotation scanning.
     */
    private ResourceLoader resourceLoader;

    /**
     * Environment for {@link LepServiceProvider}.
     */
    private Environment environment;

    /**
     * {@inheritDoc}
     */
    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        LepServiceProvider scanner = getScanner();
        Set<String> basePackages = getBasePackages(importingClassMetadata);
        for (String basePackage : basePackages) {
            Set<BeanDefinition> candidateComponents = scanner.findCandidateComponents(basePackage);

            for (BeanDefinition candidateComponent : candidateComponents) {
                if (candidateComponent instanceof AnnotatedBeanDefinition) {
                    AnnotatedBeanDefinition beanDefinition = (AnnotatedBeanDefinition) candidateComponent;
                    AnnotationMetadata annotationMetadata = beanDefinition.getMetadata();

                    Map<String, Object> attributes = annotationMetadata
                        .getAnnotationAttributes(LepService.class.getCanonicalName());

                    registerLepService(registry, annotationMetadata, attributes);
                }
            }
        }
    }

    private void registerLepService(BeanDefinitionRegistry registry,
                                    AnnotationMetadata annotationMetadata,
                                    Map<String, Object> attributes) {
        // has a default, won't be null
        boolean primary = (Boolean) attributes.get("primary");

        String className = annotationMetadata.getClassName();
        BeanDefinitionBuilder definition = BeanDefinitionBuilder
            .genericBeanDefinition(LepServiceFactoryBean.class);
        definition.addPropertyValue("type", className);
        definition.addPropertyValue("annotationMetadata", annotationMetadata);
        definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);

        AbstractBeanDefinition beanDefinition = definition.getBeanDefinition();
        beanDefinition.setPrimary(primary);

        String name = getBeanName(annotationMetadata, attributes);
        String alias = name + LepService.class.getSimpleName();
        String qualifier = getQualifier(attributes);
        if (StringUtils.hasText(qualifier)) {
            alias = qualifier;
        }

        BeanDefinitionHolder holder = new BeanDefinitionHolder(beanDefinition, name,
                                                               new String[] {alias});
        BeanDefinitionReaderUtils.registerBeanDefinition(holder, registry);
    }

    private static String getBeanName(AnnotationMetadata annotationMetadata, Map<String, Object> attributes) {
        // name & value are aliases
        String beanName = (String) attributes.get("value");
        if (!StringUtils.isEmpty(beanName)) {
            return beanName;
        } else {
            // generate bean name from class name
            String shortName = ClassUtils.getShortName(annotationMetadata.getClassName());
            return StringUtils.uncapitalize(shortName);
        }
    }

    private static String getQualifier(Map<String, Object> lepServiceAttributes) {
        if (lepServiceAttributes == null) {
            return null;
        }
        String qualifier = (String) lepServiceAttributes.get("qualifier");
        if (StringUtils.hasText(qualifier)) {
            return qualifier;
        }
        return null;
    }

    private LepServiceProvider getScanner() {
        LepServiceProvider scanner = new LepServiceProvider(this.environment);
        scanner.setResourceLoader(this.resourceLoader);
        return scanner;
    }

    private Set<String> getBasePackages(AnnotationMetadata importingClassMetadata) {
        Map<String, Object> attributes = importingClassMetadata
            .getAnnotationAttributes(EnableLepServices.class.getCanonicalName());

        Set<String> basePackages = new HashSet<>();
        for (String pkg : (String[]) attributes.get("value")) {
            if (StringUtils.hasText(pkg)) {
                basePackages.add(pkg);
            }
        }

        for (String pkg : (String[]) attributes.get("basePackages")) {
            if (StringUtils.hasText(pkg)) {
                basePackages.add(pkg);
            }
        }

        for (Class<?> clazz : (Class[]) attributes.get("basePackageClasses")) {
            basePackages.add(ClassUtils.getPackageName(clazz));
        }

        if (basePackages.isEmpty()) {
            basePackages.add(
                ClassUtils.getPackageName(importingClassMetadata.getClassName()));
        }

        return basePackages;
    }

}
