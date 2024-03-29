package com.icthh.xm.commons.permission.config;

import com.icthh.xm.commons.logging.util.BasePackageDetector;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class ReflectionConfig {

    private final String scanPackage;

    public ReflectionConfig(BasePackageDetector basePackageDetector, @Value("${base-package:'-'}") String scanPackage) {
        if (scanPackage.equals("'-'")) {
            this.scanPackage = basePackageDetector.getBasePackage();
        } else {
            this.scanPackage = scanPackage;
        }
    }

    /**
     * {@link Reflections} bean.
     *
     * @return bean
     */
    @Bean
    public Reflections reflections() {
        log.info("Configuring reflections to scan permissions in package: {}", scanPackage);
        return new Reflections(new ConfigurationBuilder()
                                    .setUrls(ClasspathHelper.forPackage(scanPackage.trim()))
                                    .setScanners(new MethodAnnotationsScanner())
                                    .filterInputsBy(new FilterBuilder().include(".*class")));
    }
}
