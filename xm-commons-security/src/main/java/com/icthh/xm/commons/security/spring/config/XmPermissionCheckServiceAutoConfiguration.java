package com.icthh.xm.commons.security.spring.config;

import com.icthh.xm.commons.security.XmPermissionCheckService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class XmPermissionCheckServiceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public XmPermissionCheckService xmPermissionCheckService() {
        return (privilege, context) -> false;
    }
}
