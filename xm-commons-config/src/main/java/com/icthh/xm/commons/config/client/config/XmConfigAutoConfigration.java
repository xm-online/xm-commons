package com.icthh.xm.commons.config.client.config;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.icthh.xm.commons.config.client.repository.TenantConfigRepository;
import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.loadbalancer.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

@Configuration
@ConditionalOnProperty("xm-config.enabled")
public class XmConfigAutoConfigration {

    public static final String XM_CONFIG_REST_TEMPLATE = "xm-config-rest-template";

    @Bean
    public TenantListRepository tenantListRepository(@Qualifier(XM_CONFIG_REST_TEMPLATE) RestTemplate restTemplate,
                                                     @Value("${spring.application.name}") String applicationName,
                                                     XmConfigProperties xmConfigProperties) {
        return new TenantListRepository(restTemplate, applicationName, xmConfigProperties);
    }

    @Bean
    public TenantConfigRepository tenantConfigRepository(@Qualifier(XM_CONFIG_REST_TEMPLATE) RestTemplate restTemplate,
                                                         @Value("${spring.application.name}") String applicationName,
                                                         XmConfigProperties xmConfigProperties) {
        return new TenantConfigRepository(restTemplate, applicationName, xmConfigProperties);
    }

    @Bean(XM_CONFIG_REST_TEMPLATE)
    public RestTemplate restTemplate(RestTemplateCustomizer customizer) {
        RestTemplate restTemplate = new RestTemplate();
        customizer.customize(restTemplate);
        restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(UTF_8));
        return restTemplate;
    }

}
