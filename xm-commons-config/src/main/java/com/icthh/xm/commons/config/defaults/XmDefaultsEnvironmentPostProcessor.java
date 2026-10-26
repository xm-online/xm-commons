package com.icthh.xm.commons.config.defaults;

import lombok.SneakyThrows;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.Profiles;
import org.springframework.core.io.ClassPathResource;

import java.util.List;

public class XmDefaultsEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    public static final String MIGRATION_PROFILE = "migration";

    private static final String LOCATION = "xm-defaults/";
    private static final String DEFAULTS_FILE = "xm-defaults.yml";

    private static final List<String> PROFILES = List.of(MIGRATION_PROFILE, "prod");

    private final YamlPropertySourceLoader loader = new YamlPropertySourceLoader();

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        MutablePropertySources propertySources = environment.getPropertySources();
        PROFILES.stream()
            .filter(profile -> environment.acceptsProfiles(Profiles.of(profile)))
            .forEach(profile -> addLast(propertySources, "xm-defaults-" + profile + ".yml"));
        addLast(propertySources, DEFAULTS_FILE);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @SneakyThrows
    private void addLast(MutablePropertySources propertySources, String file) {
        loader.load(file, new ClassPathResource(LOCATION + file)).forEach(propertySources::addLast);
    }
}
