package com.icthh.xm.commons.config.defaults;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.ConditionalOnDiscoveryEnabled;
import org.springframework.cloud.consul.ConditionalOnConsulEnabled;
import org.springframework.cloud.consul.ConsulClient;
import org.springframework.cloud.consul.discovery.ConsulDiscoveryProperties;
import org.springframework.cloud.consul.discovery.HeartbeatProperties;
import org.springframework.cloud.consul.discovery.ReregistrationPredicate;
import org.springframework.cloud.consul.discovery.TtlScheduler;
import org.springframework.cloud.consul.serviceregistry.ApplicationStatusProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The TtlScheduler of ConsulHeartbeatAutoConfiguration with {@link ConsulCheckFailureClient}, so
 * spring.cloud.consul.discovery.heartbeat.reregister-service-on-failure registers the service again.
 * <p>
 * The client is decorated here and not by a BeanPostProcessor: with consul config the client is a singleton
 * registered from the bootstrap context, BeanPostProcessors do not see it.
 * Remove when https://github.com/spring-cloud/spring-cloud-consul/issues/954 is fixed.
 */
@Configuration
@ConditionalOnClass(name = "org.springframework.cloud.consul.discovery.TtlScheduler")
@ConditionalOnConsulEnabled
@ConditionalOnDiscoveryEnabled
@ConditionalOnProperty("spring.cloud.consul.discovery.heartbeat.enabled")
public class XmConsulHeartbeatConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TtlScheduler ttlScheduler(HeartbeatProperties heartbeatProperties,
                                     ConsulDiscoveryProperties discoveryProperties, ConsulClient consulClient,
                                     ReregistrationPredicate reregistrationPredicate,
                                     ObjectProvider<ApplicationStatusProvider> applicationStatusProvider) {
        return new TtlScheduler(heartbeatProperties, discoveryProperties, new ConsulCheckFailureClient(consulClient),
            reregistrationPredicate, applicationStatusProvider);
    }
}
