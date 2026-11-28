package com.icthh.xm.commons.secret;

import com.icthh.xm.commons.tenant.TenantContextHolder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Plain ApplicationContextRunner: not a cached Spring test context. */
class SecretWiringUnitTest {

    @Configuration
    @EnableConfigurationProperties(SecretProperties.class)
    @Import({VaultSecretStore.class, SecretService.class})
    static class Wiring {
        @Bean
        TenantContextHolder tenantContextHolder() {
            return SecretTestSupport.tenantHolder("xm");
        }
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(Wiring.class);

    @Test
    void disabledByDefaultStartsWithoutStore() {
        runner.run(context -> {
            assertThat(context).hasNotFailed().doesNotHaveBean(SecretStore.class);
            assertThatThrownBy(() -> context.getBean(SecretService.class).get("a"))
                .hasMessageContaining("Secrets are disabled");
        });
    }

    @Test
    void enabledCreatesVaultStoreWithoutCallingVault() {
        runner.withPropertyValues("application.secrets.enabled=true", "application.secrets.authentication=token",
                "application.secrets.token=t", "application.secrets.url=http://localhost:1",
                "application.secrets.cache-ttl=5s")
            .run(context -> assertThat(context).hasNotFailed().hasSingleBean(VaultSecretStore.class));
    }
}
