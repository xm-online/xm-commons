package com.icthh.xm.commons.web.spring.config;

import com.icthh.xm.commons.config.client.config.XmConfigConfiguration;
import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.icthh.xm.commons.config.client.config.XmConfigTenantConfiguration;
import com.icthh.xm.commons.logging.spring.config.ServiceLoggingAspectConfiguration;
import com.icthh.xm.commons.logging.web.spring.config.RestLoggingAspectConfiguration;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.security.spring.config.XmAuthenticationContextConfiguration;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.spring.config.TenantContextConfiguration;
import com.icthh.xm.commons.web.spring.TenantInterceptor;
import com.icthh.xm.commons.web.spring.TenantVerifyInterceptor;
import com.icthh.xm.commons.web.spring.XmLoggingInterceptor;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;

/**
 * The {@link XmMsWebConfiguration} class.
 */
@Configuration
@Import({
    LogstashConfiguration.class,
    XmAuthenticationContextConfiguration.class,
    TenantContextConfiguration.class,
    ServiceLoggingAspectConfiguration.class,
    RestLoggingAspectConfiguration.class,
    XmConfigConfiguration.class,
    XmConfigTenantConfiguration.class
})
public class XmMsWebConfiguration {

    @Bean
    @Order(1)
    TenantInterceptor tenantInterceptor(XmAuthenticationContextHolder xmAuthenticationContextHolder,
                                        TenantContextHolder tenantContextHolder) {
        return new TenantInterceptor(xmAuthenticationContextHolder, tenantContextHolder);
    }

    @Bean
    @Order(2)
    @ConditionalOnExpression("${xm-config.enabled} && ${tenant.reject-suspended:true}")
    TenantVerifyInterceptor tenantVerifyInterceptor(@Lazy TenantListRepository tenantListRepository,
                                                    TenantContextHolder tenantContextHolder) {
        return new TenantVerifyInterceptor(tenantListRepository, tenantContextHolder);
    }

    @Bean
    XmLoggingInterceptor xmLoggingInterceptor(XmAuthenticationContextHolder xmAuthenticationContextHolder,
                                              TenantContextHolder tenantContextHolder) {
        return new XmLoggingInterceptor(xmAuthenticationContextHolder, tenantContextHolder);
    }

}
