package com.icthh.xm.commons.lep.spring;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.type.filter.AnnotationTypeFilter;

/**
 * Custom {@link ClassPathScanningCandidateComponentProvider} scanning for interfaces annotated with {@code @LepService}
 * annotation.
 */
public class LepServiceProvider extends ClassPathScanningCandidateComponentProvider {

    /**
     * Create a LepServiceProvider with the given {@link Environment}.
     *
     * @param environment the Environment to use
     */
    public LepServiceProvider(Environment environment) {
        super(false, environment);

        addIncludeFilter(new AnnotationTypeFilter(LepService.class, false));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
        return beanDefinition.getMetadata().isInterface();
    }

}
