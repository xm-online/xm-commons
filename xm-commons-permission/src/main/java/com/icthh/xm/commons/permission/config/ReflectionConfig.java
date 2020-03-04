package com.icthh.xm.commons.permission.config;

import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ReflectionConfig {

    @Value("${base-package:com.icthh.xm}")
    private String scanPackage;

    /**
     * {@link Reflections} bean.
     *
     * @return bean
     */
    @Bean
    public Reflections reflections() {
        return new Reflections(new ConfigurationBuilder()
                                    .setUrls(ClasspathHelper.forPackage(scanPackage.trim()))
                                    .setScanners(new MethodAnnotationsScanner())
                                    .filterInputsBy(new FilterBuilder().include(".*class")));
    }
}
