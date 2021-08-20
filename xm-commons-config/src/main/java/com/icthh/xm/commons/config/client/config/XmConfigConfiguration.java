package com.icthh.xm.commons.config.client.config;

import static com.icthh.xm.commons.config.client.config.XmRestTemplateConfiguration.XM_CONFIG_REST_TEMPLATE;

import com.icthh.xm.commons.config.client.api.ConfigService;
import com.icthh.xm.commons.config.client.listener.ApplicationReadyEventListener;
import com.icthh.xm.commons.config.client.repository.CommonConfigRepository;
import com.icthh.xm.commons.config.client.repository.kafka.ConfigTopicConsumer;
import com.icthh.xm.commons.config.client.service.CommonConfigService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
@Import({
    XmRestTemplateConfiguration.class
})
@ConditionalOnProperty("xm-config.enabled")
public class XmConfigConfiguration {

    @Bean
    public CommonConfigRepository commonConfigRepository(
        @Qualifier(XM_CONFIG_REST_TEMPLATE) RestTemplate restTemplate,
        XmConfigProperties xmConfigProperties) {
        return new CommonConfigRepository(restTemplate, xmConfigProperties);
    }

    @Bean
    public ConfigService configService(
        CommonConfigRepository commonConfigRepository) {
        return new CommonConfigService(commonConfigRepository);
    }

    @Bean
    public InitRefreshableConfigurationBeanPostProcessor refreshableConfigurationPostProcessor(
        ConfigService configService) {
        return new InitRefreshableConfigurationBeanPostProcessor(configService);
    }

    @Bean
    public ConfigTopicConsumer configTopicConsumer(ConfigService configService) {
        return new ConfigTopicConsumer(configService);
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
