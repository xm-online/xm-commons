package com.icthh.xm.commons.scheduler.config;

import com.icthh.xm.commons.config.client.config.XmConfigProperties;
import com.icthh.xm.commons.scheduler.adapter.DynamicTopicConsumerConfiguration;
import com.icthh.xm.commons.scheduler.adapter.SchedulerChannelManager;
import com.icthh.xm.commons.scheduler.service.SchedulerEventService;
import com.icthh.xm.commons.topic.service.DynamicConsumerConfigurationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty("application.scheduler-enabled")
public class SchedulerConfiguration {

    @Bean
    public DynamicTopicConsumerConfiguration dynamicTopicConsumerConfiguration(SchedulerEventService schedulerEventService) {
        return new DynamicTopicConsumerConfiguration(schedulerEventService);
    }

    @Bean
    public SchedulerChannelManager schedulerChannelManager(XmConfigProperties xmConfigProperties,
                                                           DynamicTopicConsumerConfiguration dynamicTopicConsumerConfiguration,
                                                           DynamicConsumerConfigurationService dynamicConsumerConfigurationService) {
        return new SchedulerChannelManager(xmConfigProperties, dynamicTopicConsumerConfiguration, dynamicConsumerConfigurationService);
    }
}
