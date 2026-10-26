package com.icthh.xm.commons.config.defaults;

import com.ecwid.consul.v1.ConsulClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.ConditionalOnDiscoveryEnabled;
import org.springframework.cloud.consul.ConditionalOnConsulEnabled;
import org.springframework.cloud.consul.discovery.ConsulDiscoveryProperties;
import org.springframework.cloud.consul.discovery.HeartbeatProperties;
import org.springframework.cloud.consul.discovery.TtlScheduler;
import org.springframework.cloud.consul.serviceregistry.ConsulAutoRegistration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(name = {
    "org.springframework.cloud.consul.discovery.TtlScheduler",
    "org.springframework.boot.actuate.health.HealthEndpoint"
})
@ConditionalOnConsulEnabled
@ConditionalOnDiscoveryEnabled
@ConditionalOnProperty("spring.cloud.consul.discovery.heartbeat.enabled")
public class XmConsulHeartbeatConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TtlScheduler ttlScheduler(HeartbeatProperties heartbeatProperties,
                                     ConsulDiscoveryProperties discoveryProperties, ConsulClient consulClient,
                                     ObjectProvider<HealthEndpoint> healthEndpoint,
                                     ObjectProvider<ConsulAutoRegistration> registration,
                                     @Value("${spring.cloud.consul.discovery.heartbeat.reregister-service-on-failure:false}")
                                         boolean reregisterServiceOnFailure) {
        return new XmTtlScheduler(heartbeatProperties, discoveryProperties, consulClient, healthEndpoint,
            registration, reregisterServiceOnFailure);
    }
}
