package com.icthh.xm.commons.tenant.spring.config;

import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.internal.DefaultTenantContextHolder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The {@link TenantContextConfiguration} class.
 */
@Configuration
public class TenantContextConfiguration {

    @Bean
    TenantContextHolder tenantContextHolder() {
        return new DefaultTenantContextHolder();
    }

}
