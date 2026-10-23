package com.icthh.xm.commons.config.client.config;

import com.icthh.xm.commons.config.client.repository.ConfigPrivateApi;
import com.icthh.xm.commons.config.client.repository.ConfigPrivateKeyNotSetException;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.HttpHeaders;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import static com.icthh.xm.commons.config.client.repository.ConfigPrivateApi.TOKEN_HEADER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class XmConfigConfigurationPrivateApiUnitTest {

    private static final String PRIVATE_KEY = "MC4CAQAwBQYDK2VwBCIEIOIvuCuM2px95QXJIqbjhd2ZvQjXq5C83PtrriD4d7ar";

    @Rule
    public TemporaryFolder configDir = new TemporaryFolder();

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class))
        .withUserConfiguration(XmConfigConfiguration.class)
        .withBean(XmConfigProperties.class, this::xmConfigProperties)
        .withBean(RestTemplateBuilder.class, () -> new RestTemplateBuilder())
        .withBean(KafkaProperties.class)
        .withBean(KafkaTemplate.class, () -> mock(KafkaTemplate.class))
        .withBean(ConsumerFactory.class, () -> mock(ConsumerFactory.class))
        .withBean(TenantContextHolder.class, () -> mock(TenantContextHolder.class))
        .withPropertyValues("xm-config.enabled=true", "spring.cloud.loadbalancer.enabled=false",
            "spring.application.name=test");

    @Test
    public void testOldApiWithMigrationProfile() {
        runner.withPropertyValues("spring.profiles.active=migration").run(context -> {
            ConfigPrivateApi privateApi = context.getBean(ConfigPrivateApi.class);
            assertThat(privateApi.getUrl()).isEqualTo("/api/private");
            assertThat(privateApi.authorize(new HttpHeaders()).containsHeader(TOKEN_HEADER)).isFalse();
        });
    }

    @Test
    public void testApiWithTokenWithoutMigrationProfile() {
        runner.withPropertyValues("spring.profiles.active=prod", "application.config-private-key=" + PRIVATE_KEY)
            .run(context -> {
                ConfigPrivateApi privateApi = context.getBean(ConfigPrivateApi.class);
                assertThat(privateApi.getUrl()).isEqualTo("/api/private/v2");
                assertThat(privateApi.authorize(new HttpHeaders()).containsHeader(TOKEN_HEADER)).isTrue();
            });
    }

    @Test
    public void testStartupFailsWithoutPrivateKeyAndMigrationProfile() {
        runner.withPropertyValues("spring.profiles.active=prod").run(context ->
            assertThat(context).getFailure().rootCause().isInstanceOf(ConfigPrivateKeyNotSetException.class));
    }

    @Test
    public void testFileModeNeedsNoPrivateKey() {
        runner.withPropertyValues("spring.profiles.active=prod", "xm-config.configMode=FILE").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(ConfigPrivateApi.class);
        });
    }

    private XmConfigProperties xmConfigProperties() {
        XmConfigProperties properties = new XmConfigProperties();
        properties.setConfigDirPath(configDir.getRoot().getAbsolutePath());
        return properties;
    }
}
