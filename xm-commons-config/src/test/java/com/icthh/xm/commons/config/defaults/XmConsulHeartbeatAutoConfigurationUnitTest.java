package com.icthh.xm.commons.config.defaults;

import org.junit.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.commons.util.UtilAutoConfiguration;
import org.springframework.cloud.consul.ConsulAutoConfiguration;
import org.springframework.cloud.consul.discovery.ConsulDiscoveryClientConfiguration;
import org.springframework.cloud.consul.discovery.TtlScheduler;
import org.springframework.cloud.consul.support.ConsulHeartbeatAutoConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class XmConsulHeartbeatAutoConfigurationUnitTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(UtilAutoConfiguration.class, ConsulAutoConfiguration.class,
            ConsulDiscoveryClientConfiguration.class, ConsulHeartbeatAutoConfiguration.class,
            XmConsulHeartbeatAutoConfiguration.class));

    @Test
    public void testTtlSchedulerUsesDecoratedClient() {
        runner.withPropertyValues("spring.cloud.consul.discovery.heartbeat.enabled=true").run(context -> {
            assertThat(context).hasSingleBean(TtlScheduler.class);
            Object client = ReflectionTestUtils.getField(context.getBean(TtlScheduler.class), "client");
            assertThat(client).isInstanceOf(ConsulCheckFailureClient.class);
        });
    }

    @Test
    public void testNoTtlSchedulerWithoutHeartbeat() {
        runner.withPropertyValues("spring.cloud.consul.discovery.heartbeat.enabled=false")
            .run(context -> assertThat(context).doesNotHaveBean(TtlScheduler.class));
    }

    @Test
    public void testNoTtlSchedulerWhenConsulIsDisabled() {
        runner.withPropertyValues("spring.cloud.consul.discovery.heartbeat.enabled=true",
                "spring.cloud.consul.enabled=false")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean(TtlScheduler.class);
            });
    }
}
