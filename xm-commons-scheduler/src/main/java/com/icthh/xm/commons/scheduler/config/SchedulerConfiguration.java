package com.icthh.xm.commons.scheduler.config;

import com.icthh.xm.commons.config.client.config.XmConfigProperties;
import com.icthh.xm.commons.scheduler.adapter.DynamicTopicConsumerConfiguration;
import com.icthh.xm.commons.scheduler.adapter.SchedulerChannelManager;
import com.icthh.xm.commons.scheduler.service.SchedulerEventService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty("application.scheduler-enabled")
public class SchedulerConfiguration {

    @Bean
    public DynamicTopicConsumerConfiguration dynamicTopicConsumerConfiguration(SchedulerEventService schedulerEventService,
                                                                               ApplicationEventPublisher applicationEventPublisher) {
        return new DynamicTopicConsumerConfiguration(schedulerEventService, applicationEventPublisher);
    }

    @Bean
    public SchedulerChannelManager schedulerChannelManager(XmConfigProperties xmConfigProperties,
                                                           DynamicTopicConsumerConfiguration dynamicTopicConsumerConfiguration) {
        return new SchedulerChannelManager(xmConfigProperties, dynamicTopicConsumerConfiguration);
    }
}
