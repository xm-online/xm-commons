package com.icthh.xm.commons.config.client.config;

import static com.icthh.xm.commons.config.client.config.XmRestTemplateConfiguration.XM_CONFIG_REST_TEMPLATE;

import com.icthh.xm.commons.config.client.api.ConfigService;
import com.icthh.xm.commons.config.client.listener.ApplicationReadyEventListener;
import com.icthh.xm.commons.config.client.repository.ConfigRepository;
import com.icthh.xm.commons.config.client.repository.ConfigurationModel;
import com.icthh.xm.commons.config.client.repository.kafka.ConfigTopicConsumer;
import com.icthh.xm.commons.config.client.service.ConfigServiceImpl;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Configuration
@Import({
    XmRestTemplateConfiguration.class
})
@ConditionalOnProperty("xm-config.enabled")
public class XmConfigConfiguration {

    @Bean
    public ConfigRepository configRepository(
        @Qualifier(XM_CONFIG_REST_TEMPLATE) RestTemplate restTemplate,
        XmConfigProperties xmConfigProperties) {
        return new ConfigRepository(restTemplate, xmConfigProperties);
    }

    @Bean
    public ConfigService configService(
        ConfigRepository configRepository) {
        return new ConfigServiceImpl(configRepository);
    }

    @Bean
    public InitRefreshableConfigurationBeanPostProcessor refreshableConfigurationPostProcessor(
        ConfigService configService,
        ConfigurationModel configurationModel) {
        return new InitRefreshableConfigurationBeanPostProcessor(configService, configurationModel);
    }

    @Bean
    public ConfigTopicConsumer configTopicConsumer(ConfigurationModel configurationModel) {
        return new ConfigTopicConsumer(configurationModel);
    }

    @Bean
    public ApplicationReadyEventListener applicationReadyEventListener(
        ConsumerFactory<String, String> consumerFactory,
        ConfigTopicConsumer configTopicConsumer,
        KafkaProperties kafkaProperties,
        XmConfigProperties xmConfigProperties) {
        return new ApplicationReadyEventListener(consumerFactory, configTopicConsumer, kafkaProperties, xmConfigProperties);
    }
}
