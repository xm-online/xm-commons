package com.icthh.xm.commons.lep.spring.web;

import com.icthh.xm.commons.lep.spring.LepSpringConfiguration;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

/**
 * The {@link WebLepSpringConfiguration} class.
 */
@Configuration
public abstract class WebLepSpringConfiguration extends LepSpringConfiguration {

    public WebLepSpringConfiguration(String appName,
                                     ApplicationEventPublisher eventPublisher,
                                     ResourceLoader resourceLoader) {
        super(appName, eventPublisher, resourceLoader);
    }

    @Bean
    LepInterceptor lepInterceptor(TenantContextHolder tenantContextHolder,
                                  XmAuthenticationContextHolder xmAuthContextHolder) {
        return new LepInterceptor(lepManager(), tenantContextHolder, xmAuthContextHolder);
    }

}
