package com.icthh.xm.commons.config.client.config;

import static com.icthh.xm.commons.config.client.config.XmRestTemplateConfiguration.XM_CONFIG_REST_TEMPLATE;
import static com.icthh.xm.commons.config.defaults.XmDefaultsEnvironmentPostProcessor.MIGRATION_PROFILE;

import com.icthh.xm.commons.config.client.api.ConfigService;
import com.icthh.xm.commons.config.client.api.FetchConfigurationSettings;
import com.icthh.xm.commons.config.client.listener.ApplicationReadyEventListener;
import com.icthh.xm.commons.config.client.repository.CommonConfigRepository;
import com.icthh.xm.commons.config.client.repository.ConfigPrivateApi;
import com.icthh.xm.commons.config.client.repository.FileCommonConfigRepository;
import com.icthh.xm.commons.config.client.repository.XmMsConfigCommonConfigRepository;
import com.icthh.xm.commons.config.client.repository.file.FileUpdateWatcher;
import com.icthh.xm.commons.config.client.repository.kafka.ConfigTopicConsumer;
import com.icthh.xm.commons.config.client.service.CommonConfigService;
import com.icthh.xm.commons.config.client.state.ConfigStateHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.client.RestTemplate;

@Configuration
@Import({
    XmRestTemplateConfiguration.class
})
@ConditionalOnProperty("xm-config.enabled")
public class XmConfigConfiguration {

    @Bean
    @ConditionalOnProperty(value = "xm-config.configMode", havingValue = "XM_MS_CONFIG", matchIfMissing = true)
    public CommonConfigRepository commonConfigRepository(
        @Qualifier(XM_CONFIG_REST_TEMPLATE) RestTemplate restTemplate,
        XmConfigProperties xmConfigProperties,
        KafkaTemplate<String, String> kafkaTemplate,
        TenantContextHolder tenantContextHolder,
        ConfigPrivateApi privateApi) {
        return new XmMsConfigCommonConfigRepository(restTemplate, xmConfigProperties, kafkaTemplate, tenantContextHolder,
            privateApi);
    }

    @Bean
    @Profile(MIGRATION_PROFILE)
    @ConditionalOnProperty(value = "xm-config.configMode", havingValue = "XM_MS_CONFIG", matchIfMissing = true)
    public ConfigPrivateApi migrationConfigPrivateApi(@Value("${spring.application.name}") String applicationName) {
        return new ConfigPrivateApi(true, applicationName, null);
    }

    @Bean
    @Profile("!" + MIGRATION_PROFILE)
    @ConditionalOnProperty(value = "xm-config.configMode", havingValue = "XM_MS_CONFIG", matchIfMissing = true)
    public ConfigPrivateApi configPrivateApi(@Value("${spring.application.name}") String applicationName,
                                             @Value("${application.config-private-key:}") String configPrivateKey) {
        return new ConfigPrivateApi(false, applicationName, configPrivateKey);
    }

    @Bean
    @ConditionalOnProperty(value = "xm-config.configMode", havingValue = "FILE")
    public CommonConfigRepository fileCommonConfigRepository(XmConfigProperties xmConfigProperties) {
        return new FileCommonConfigRepository(xmConfigProperties);
    }

    @Bean
    @ConditionalOnProperty(value = "xm-config.configMode", havingValue = "FILE")
    public FileUpdateWatcher fileUpdateWatcher(ConfigService configService, XmConfigProperties xmConfigProperties) {
        return new FileUpdateWatcher(configService, xmConfigProperties);
    }

    @Bean
    public FetchConfigurationSettings fetchConfigurationSettings(@Value("${spring.application.name}") String applicationName,
                                                                 @Value("${application.config-fetch-all.enabled:false}") Boolean isFetchAll) {
        return new FetchConfigurationSettings(applicationName, isFetchAll);
    }

    @Bean
    public ConfigStateHolder configStateHolder() {
        return new ConfigStateHolder();
    }

    @Bean
    public ConfigService configService(
        CommonConfigRepository commonConfigRepository,
        FetchConfigurationSettings fetchConfigurationSettings,
        ConfigStateHolder configStateHolder) {
        return new CommonConfigService(fetchConfigurationSettings, commonConfigRepository, configStateHolder);
    }

    @Bean
    public InitRefreshableConfigurationBeanPostProcessor refreshableConfigurationPostProcessor(
            ObjectProvider<ConfigService> configServiceProvider, XmConfigProperties xmConfigProperties,
            FetchConfigurationSettings fetchConfigurationSettings,
            ObjectProvider<ConfigStateHolder> configStateHolderProvider) {
        return new InitRefreshableConfigurationBeanPostProcessor(configServiceProvider, xmConfigProperties,
            fetchConfigurationSettings, configStateHolderProvider);
    }

    @Bean
    public ConfigTopicConsumer configTopicConsumer(ConfigService configService) {
        return new ConfigTopicConsumer(configService);
    }

    @Bean
    public ApplicationReadyEventListener applicationReadyEventListener(
        ConsumerFactory<String, String> consumerFactory,
        ConfigTopicConsumer configTopicConsumer,
        XmConfigProperties xmConfigProperties) {
        return new ApplicationReadyEventListener(consumerFactory, configTopicConsumer, xmConfigProperties);
    }
}
