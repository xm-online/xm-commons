package com.icthh.xm.commons.config.client.config;

import com.icthh.xm.commons.config.client.repository.CommonConfigRepository;
import com.icthh.xm.commons.config.client.repository.TenantConfigRepository;
import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import com.icthh.xm.commons.config.client.service.TenantAliasService;
import com.icthh.xm.commons.config.client.service.TenantAliasServiceConfiguration;
import com.icthh.xm.commons.config.client.service.TenantAliasServiceImpl;
import com.icthh.xm.commons.config.client.service.TenantConfigService;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestTemplate;

import static com.icthh.xm.commons.config.client.config.XmRestTemplateConfiguration.XM_CONFIG_REST_TEMPLATE;

@Configuration
@Import({
    XmRestTemplateConfiguration.class,
    XmConfigConfiguration.class
})
@ConditionalOnExpression("${xm-config.enabled} && ${tenant.reject-suspended:true}")
public class XmConfigTenantConfiguration {

    @Bean
    public TenantListRepository tenantListRepository(@Qualifier(XM_CONFIG_REST_TEMPLATE) RestTemplate restTemplate,
                                                     @Value("${spring.application.name}") String applicationName,
                                                     CommonConfigRepository commonConfigRepository,
                                                     XmConfigProperties xmConfigProperties) {
        return new TenantListRepository(restTemplate, commonConfigRepository, applicationName, xmConfigProperties);
    }

    @Bean
    public TenantAliasServiceConfiguration tenantAliasServiceConfiguration(TenantAliasService tenantAliasService) {
        return new TenantAliasServiceConfiguration(tenantAliasService);
    }

    @Bean
    public TenantAliasService tenantAliasService(CommonConfigRepository commonConfigRepository,
                                                 TenantListRepository tenantListRepository) {
        return new TenantAliasServiceImpl(commonConfigRepository, tenantListRepository);
    }

    @Bean
    public TenantConfigRepository tenantConfigRepository(@Qualifier(XM_CONFIG_REST_TEMPLATE) RestTemplate restTemplate,
                                                         @Value("${spring.application.name}") String applicationName,
                                                         XmConfigProperties xmConfigProperties) {
        return new TenantConfigRepository(restTemplate, applicationName, xmConfigProperties);
    }

    @Bean
    @ConditionalOnMissingBean(TenantConfigService.class)
    public TenantConfigService tenantConfigService(XmConfigProperties xmConfigProperties,
                                                   TenantContextHolder tenantContextHolder) {
        return new TenantConfigService(xmConfigProperties, tenantContextHolder);
    }

}
