package com.icthh.xm.commons.security.spring.config;

import java.util.Set;
import org.springframework.boot.autoconfigure.AutoConfigurationImportFilter;
import org.springframework.boot.autoconfigure.AutoConfigurationMetadata;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

/**
 * Skips Spring Boot auto-configurations that are not used by XM microservices.
 * Can be disabled with {@code application.xm-auto-configuration-filter.disabled=true}.
 */
public class XmAutoConfigurationImportFilter implements AutoConfigurationImportFilter, EnvironmentAware {

    public static final String DISABLED_PROPERTY = "application.xm-auto-configuration-filter.disabled";

    private static final Set<String> EXCLUDED_AUTO_CONFIGURATIONS = Set.of(
        "org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration"
    );

    private boolean disabled;

    @Override
    public void setEnvironment(Environment environment) {
        this.disabled = environment.getProperty(DISABLED_PROPERTY, Boolean.class, false);
    }

    @Override
    public boolean[] match(String[] autoConfigurationClasses, AutoConfigurationMetadata autoConfigurationMetadata) {
        boolean[] matches = new boolean[autoConfigurationClasses.length];
        for (int i = 0; i < autoConfigurationClasses.length; i++) {
            // null is a candidate that an earlier filter already skipped
            String autoConfigurationClass = autoConfigurationClasses[i];
            matches[i] = disabled || autoConfigurationClass == null
                || !EXCLUDED_AUTO_CONFIGURATIONS.contains(autoConfigurationClass);
        }
        return matches;
    }
}
