package com.icthh.xm.commons.config.client.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.loadbalancer.LoadBalancerAutoConfiguration;
import org.springframework.cloud.client.loadbalancer.RestTemplateCustomizer;
import org.springframework.cloud.client.loadbalancer.RetryLoadBalancerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Ribbon interceptor.
 * Allowing to disable Ribbon interceptor by property ribbon.http.client.enabled = false
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "ribbon.http.client.enabled", havingValue = "false")
public class DisabledRibbonInterceptorConfiguration
    extends LoadBalancerAutoConfiguration.RetryInterceptorAutoConfiguration {

    @Bean
    public RestTemplateCustomizer restTemplateCustomizer(final RetryLoadBalancerInterceptor loadBalancerInterceptor) {
        return restTemplate ->
            log.warn("No loadBalancerInterceptor is injected due to ribbon.http.client.enabled = false");
    }

}


