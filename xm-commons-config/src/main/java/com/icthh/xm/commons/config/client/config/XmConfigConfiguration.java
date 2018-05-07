package com.icthh.xm.commons.config.client.config;

import static com.icthh.xm.commons.config.client.config.XmRestTemplateConfiguration.XM_CONFIG_REST_TEMPLATE;

import com.icthh.xm.commons.config.client.api.ConfigService;
import com.icthh.xm.commons.config.client.repository.ConfigRepository;
import com.icthh.xm.commons.config.client.service.ConfigServiceImpl;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestTemplate;

@Configuration
@Import({
    XmRestTemplateConfiguration.class
})
@ConditionalOnProperty("xm-config.enabled")
public class XmConfigConfiguration {

    @Bean
    public ConfigRepository configRepository(@Qualifier(XM_CONFIG_REST_TEMPLATE) RestTemplate restTemplate,
                                            XmConfigProperties xmConfigProperties) {
        return new ConfigRepository(restTemplate, xmConfigProperties);
    }

    @Bean
    public ConfigService configService(ConfigRepository configRepository) {
        return new ConfigServiceImpl(configRepository);
    }
}
