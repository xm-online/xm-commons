package com.icthh.xm.commons.config.defaults;

import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class XmDefaultsEnvironmentPostProcessorUnitTest {

    private static final String HEARTBEAT = "spring.cloud.consul.discovery.heartbeat.enabled";
    private static final String REREGISTER = "spring.cloud.consul.discovery.heartbeat.reregister-service-on-failure";
    private static final String CRITICAL_TIMEOUT = "spring.cloud.consul.discovery.health-check-critical-timeout";
    private static final String QUERY_PASSING = "spring.cloud.consul.discovery.query-passing";
    private static final String REGISTER_MANAGEMENT =
        "spring.cloud.service-registry.auto-registration.register-management";
    private static final String MANAGEMENT_PORT = "management.server.port";
    private static final String SHOW_ACTUATOR = "springdoc.show-actuator";
    private static final String PROMETHEUS_PUBLIC = "application.prometheus.public";
    private static final String MANAGEMENT_ROLE = "application.management.access.role";
    private static final String INFO_PUBLIC = "application.management.info.public";
    private static final String API_DOCS_PRIVILEGE_CHECK = "application.api-docs.privilege-check";
    private static final String HEALTH_SHOW_DETAILS = "management.endpoint.health.show-details";
    private static final String HEALTH_ROLES = "management.endpoint.health.roles";

    @Test
    public void testDefaultsWithoutProfile() {
        StandardEnvironment environment = postProcess();

        assertThat(environment.getProperty(HEARTBEAT)).isEqualTo("true");
        assertThat(environment.getProperty(REREGISTER)).isEqualTo("true");
        assertThat(environment.getProperty("spring.cloud.consul.discovery.heartbeat.ttl")).isEqualTo("30s");
        assertThat(environment.getProperty(CRITICAL_TIMEOUT)).isEqualTo("2m");
        assertThat(environment.getProperty(QUERY_PASSING)).isEqualTo("true");
        assertThat(environment.getProperty(REGISTER_MANAGEMENT)).isEqualTo("false");
        assertThat(environment.getProperty(SHOW_ACTUATOR)).isEqualTo("false");
        assertThat(environment.getProperty("springdoc.use-management-port")).isEqualTo("false");
        assertThat(environment.getProperty(MANAGEMENT_PORT)).isNull();
        assertThat(environment.getProperty(PROMETHEUS_PUBLIC)).isNull();
        assertThat(environment.getProperty(MANAGEMENT_ROLE)).isNull();
        assertThat(environment.getProperty(INFO_PUBLIC)).isNull();
        assertThat(environment.getProperty(API_DOCS_PRIVILEGE_CHECK)).isNull();
        assertThat(environment.getProperty(HEALTH_SHOW_DETAILS)).isEqualTo("when_authorized");
        assertThat(environment.getProperty(HEALTH_ROLES)).isEqualTo("SYSTEM-ADMIN");
    }

    @Test
    public void testProdMovesManagementToSeparatePort() {
        StandardEnvironment environment = postProcess("prod");

        assertThat(environment.getProperty(MANAGEMENT_PORT, Integer.class)).isEqualTo(9080);
        assertThat(environment.getProperty(HEARTBEAT)).isEqualTo("true");
    }

    @Test
    public void testMigrationRestoresBehaviourBeforeDefaults() {
        StandardEnvironment environment = postProcess("prod", "migration");

        assertThat(environment.getProperty(MANAGEMENT_PORT, Integer.class)).isNull();
        assertThat(Binder.get(environment).bind(MANAGEMENT_PORT, Bindable.of(Integer.class)).isBound()).isFalse();
        assertThat(environment.getProperty(HEARTBEAT)).isEqualTo("false");
        assertThat(environment.getProperty(REREGISTER)).isEqualTo("false");
        assertThat(environment.getProperty(CRITICAL_TIMEOUT)).isEmpty();
        assertThat(environment.getProperty(QUERY_PASSING)).isEqualTo("false");
        assertThat(environment.getProperty(REGISTER_MANAGEMENT)).isEqualTo("true");
        assertThat(environment.getProperty(PROMETHEUS_PUBLIC)).isEqualTo("true");
        assertThat(environment.getProperty(MANAGEMENT_ROLE)).isEqualTo("SUPER-ADMIN");
        assertThat(environment.getProperty(INFO_PUBLIC)).isEqualTo("true");
        assertThat(environment.getProperty(API_DOCS_PRIVILEGE_CHECK)).isEqualTo("false");
        assertThat(environment.getProperty(HEALTH_SHOW_DETAILS)).isEqualTo("when_authorized");
        assertThat(environment.getProperty(HEALTH_ROLES)).isEqualTo("SUPER-ADMIN");
    }

    @Test
    public void testMigrationWinsOverProdInAnyProfileOrder() {
        StandardEnvironment environment = postProcess("migration", "prod");

        assertThat(environment.getProperty(MANAGEMENT_PORT, Integer.class)).isNull();
        assertThat(environment.getProperty(HEARTBEAT)).isEqualTo("false");
    }

    @Test
    public void testServiceValueWinsOverDefaults() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.setActiveProfiles("prod");
        environment.getPropertySources().addFirst(new MapPropertySource("application.yml",
            Map.of(MANAGEMENT_PORT, "9999", SHOW_ACTUATOR, "true", HEARTBEAT, "false")));

        new XmDefaultsEnvironmentPostProcessor().postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty(MANAGEMENT_PORT)).isEqualTo("9999");
        assertThat(environment.getProperty(SHOW_ACTUATOR)).isEqualTo("true");
        assertThat(environment.getProperty(HEARTBEAT)).isEqualTo("false");
        assertThat(environment.getProperty(QUERY_PASSING)).isEqualTo("true");
    }

    @Test
    public void testRepeatedPostProcessingDoesNotDuplicateSources() {
        StandardEnvironment environment = postProcess("prod");
        int size = environment.getPropertySources().size();

        new XmDefaultsEnvironmentPostProcessor().postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getPropertySources().size()).isEqualTo(size);
    }

    private static StandardEnvironment postProcess(String... profiles) {
        StandardEnvironment environment = new StandardEnvironment();
        environment.setActiveProfiles(profiles);
        new XmDefaultsEnvironmentPostProcessor().postProcessEnvironment(environment, new SpringApplication());
        return environment;
    }
}
