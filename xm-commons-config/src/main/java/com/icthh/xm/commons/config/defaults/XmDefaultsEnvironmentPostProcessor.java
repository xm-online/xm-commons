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

/**
 * Adds xm-commons defaults with the lowest priority, so any value of the service (application*.yml, env) wins.
 * <p>
 * xm-defaults-{profile}.yml overrides xm-defaults.yml like application-{profile}.yml does.
 * The profile 'migration' restores the behaviour before these defaults and always wins over 'prod'.
 */
public class XmDefaultsEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    static final String LOCATION = "xm-defaults/";
    static final String DEFAULTS_FILE = "xm-defaults.yml";

    /**
     * Highest priority first.
     */
    static final List<String> PROFILES = List.of("migration", "prod");

    private final YamlPropertySourceLoader loader = new YamlPropertySourceLoader();

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        MutablePropertySources propertySources = environment.getPropertySources();
        PROFILES.stream()
            .filter(profile -> environment.acceptsProfiles(Profiles.of(profile)))
            .forEach(profile -> addLast(propertySources, "xm-defaults-" + profile + ".yml"));
        addLast(propertySources, DEFAULTS_FILE);
    }

    /**
     * Runs after ConfigDataEnvironmentPostProcessor, when the active profiles are known.
     */
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @SneakyThrows
    private void addLast(MutablePropertySources propertySources, String file) {
        loader.load(file, new ClassPathResource(LOCATION + file)).forEach(propertySources::addLast);
    }
}
