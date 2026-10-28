package com.icthh.xm.commons.security.spring.config;

import com.icthh.xm.commons.security.XmPermissionCheckService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class XmPermissionCheckServiceConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public XmPermissionCheckService xmPermissionCheckService() {
        return (privilege, context) -> false;
    }
}
