package com.icthh.xm.commons.scheduler.adapter;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.binder.ConsumerProperties;
import org.springframework.cloud.stream.binding.BindingService;
import org.springframework.cloud.stream.binding.BindingTargetFactory;
import org.springframework.cloud.stream.config.BindingProperties;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.SubscribableChannel;

import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.lowerCase;

@EnableBinding
@Configuration
public class BindingConfiguration {


    private static final String PREFIX = "scheduler_";

    private static final String QUEUE = "queue";

    private static final String DELIMITER = "_";

    @Autowired
    private ApplicationContext applicationContext;

    @Value("${spring.application.name}")
    private String appName;

    {
        String tenant;
        String generalQueue = PREFIX + lowerCase(tenant);


        @Override
        public String resolve(final TaskDTO task) {
        return Optional.ofNullable(task.getTenant())
            .map(tenant -> PREFIX
                + lowerCase(tenant)
                + appendTargetMs(task.getTargetMs())
                + getScheduleType(task.getChannelType()))

            .orElseThrow(() -> new RuntimeException("Tenant can not be empty"));
    }

        private String getScheduleType(ChannelType type) {
        return Optional.ofNullable(type)
            .map(Enum::toString)
            .map(String::toLowerCase)
            .orElse(QUEUE);
    }

        private String appendTargetMs(String targetMs) {
        String appendStr = DELIMITER;
        if (!StringUtils.isEmpty(targetMs)) {
            appendStr += targetMs + DELIMITER;
        }
        return appendStr;
    }
    }

    @Bean
    @Qualifier("schedulerGeneralQueue")
    public SubscribableChannel schedulerGeneralQueue(BindingServiceProperties bindingServiceProperties,
                                                        BindingTargetFactory bindingTargetFactory,
                                                        BindingService bindingService)  {
        ConsumerProperties consumerProperties = new ConsumerProperties();
        consumerProperties.setMaxAttempts(Integer.MAX_VALUE);
        BindingProperties bindingProperties = new BindingProperties();
        bindingProperties.setConsumer(consumerProperties);
        bindingProperties.setDestination(TEST_TOPIC);
        bindingProperties.setGroup("groupname");
        bindingServiceProperties.getBindings().put(TEST_TOPIC, bindingProperties);

        SubscribableChannel channel = (SubscribableChannel)bindingTargetFactory.createInput(TEST_TOPIC);
        bindingService.bindConsumer(channel, TEST_TOPIC);
        return channel;

    }

    @Bean
    @Qualifier("schedulerMicroserviceQueue")
    public SubscribableChannel schedulerMicroserviceQueue(BindingServiceProperties bindingServiceProperties,
                                                        BindingTargetFactory bindingTargetFactory,
                                                        BindingService bindingService)  {
        ConsumerProperties consumerProperties = new ConsumerProperties();
        consumerProperties.setMaxAttempts(Integer.MAX_VALUE);
        BindingProperties bindingProperties = new BindingProperties();
        bindingProperties.setConsumer(consumerProperties);
        bindingProperties.setDestination(TEST_TOPIC);
        bindingProperties.setGroup("groupname");
        bindingServiceProperties.getBindings().put(TEST_TOPIC, bindingProperties);

        SubscribableChannel channel = (SubscribableChannel)bindingTargetFactory.createInput(TEST_TOPIC);
        bindingService.bindConsumer(channel, TEST_TOPIC);
        return channel;

    }

}
