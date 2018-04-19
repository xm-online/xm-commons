package com.icthh.xm.commons.config.client.config;

import static com.icthh.xm.commons.config.client.config.XmConfigConfiguration.XM_CONFIG_REST_TEMPLATE;

import com.icthh.xm.commons.config.client.repository.TenantConfigRepository;
import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import com.icthh.xm.commons.config.client.service.TenantConfigService;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@ConditionalOnExpression("${xm-config.enabled} && ${tenant.reject-suspended:true}")
public class XmConfigAutoConfigration {

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

    @Bean
    public TenantConfigService tenantConfigService(XmConfigProperties xmConfigProperties,
                                                   TenantContextHolder tenantContextHolder) {
        return new TenantConfigService(xmConfigProperties, tenantContextHolder);
    }

}
