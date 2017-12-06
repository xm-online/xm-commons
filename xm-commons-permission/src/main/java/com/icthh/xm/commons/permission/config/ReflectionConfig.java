package com.icthh.xm.commons.permission.config;

import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ReflectionConfig {

    private static final String SCAN_PACKAGE = "com.icthh.xm";

    /**
     * {@link Reflections} bean.
     *
     * @return bean
     */
    @Bean
    public Reflections reflections() {
        return new Reflections(new ConfigurationBuilder()
                                   .setUrls(ClasspathHelper.forPackage(SCAN_PACKAGE))
                                   .setScanners(new MethodAnnotationsScanner()));
    }
}
