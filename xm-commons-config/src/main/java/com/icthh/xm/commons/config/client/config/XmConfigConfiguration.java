package com.icthh.xm.commons.config.client.config;

import static com.icthh.xm.commons.config.client.config.XmRestTemplateConfiguration.XM_CONFIG_REST_TEMPLATE;

import com.icthh.xm.commons.config.client.api.ConfigService;
import com.icthh.xm.commons.config.client.repository.ConfigRepository;
import com.icthh.xm.commons.config.client.repository.ConfigurationModel;
import com.icthh.xm.commons.config.client.service.ConfigServiceImpl;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Configuration
@Import({
    XmRestTemplateConfiguration.class
})
@ConditionalOnProperty("xm-config.enabled")
public class XmConfigConfiguration {

    public static final String CONFIG_SERVICE_LOCK = "config-service-lock";

    @Bean
    public ConfigRepository configRepository(
        @Qualifier(XM_CONFIG_REST_TEMPLATE) RestTemplate restTemplate,
        XmConfigProperties xmConfigProperties) {
        return new ConfigRepository(restTemplate, xmConfigProperties);
    }

    @Bean
    public ConfigService configService(
        XmConfigProperties xmConfigProperties,
        ConfigRepository configRepository,
        @Qualifier(CONFIG_SERVICE_LOCK) Lock lock) {
        return new ConfigServiceImpl(xmConfigProperties, configRepository, lock);
    }

    @Bean
    public InitRefreshableConfigurationBeanPostProcessor refreshableConfigurationPostProcessor(
        ConfigService configService,
        ConfigurationModel configurationModel) {
        return new InitRefreshableConfigurationBeanPostProcessor(configService, configurationModel);
    }

    @Bean
    @Qualifier(CONFIG_SERVICE_LOCK)
    public Lock configServiceLock() {
        return new ReentrantLock();
    }
}
