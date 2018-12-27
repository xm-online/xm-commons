package com.icthh.xm.commons.scheduler.adapter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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

@EnableBinding
@Configuration
public class BindingConfiguration {

    public static final String TEST_TOPIC = "test-topic";

    @Autowired
    ApplicationContext applicationContext;

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
