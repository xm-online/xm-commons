package com.icthh.xm.commons.metric;

import com.icthh.xm.commons.config.client.state.ConfigStateHolder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty("xm-config.enabled")
public class ConfigStateMetricsConfiguration {

    @Bean
    public ConfigStateMetrics configStateMetrics(ConfigStateHolder configStateHolder) {
        return new ConfigStateMetrics(configStateHolder);
    }
}
