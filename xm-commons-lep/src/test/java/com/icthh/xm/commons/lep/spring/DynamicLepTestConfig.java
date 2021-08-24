package com.icthh.xm.commons.lep.spring;

import com.icthh.xm.commons.lep.TenantScriptStorage;
import com.icthh.xm.commons.logging.config.LoggingConfigService;
import com.icthh.xm.commons.logging.config.LoggingConfigServiceStub;
import com.icthh.xm.commons.security.spring.config.XmAuthenticationContextConfiguration;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.spring.config.TenantContextConfiguration;
import com.icthh.xm.lep.api.ScopedContext;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ResourceLoader;

/**
 *
 */
@Configuration
@EnableLepServices(basePackageClasses = DynamicTestLepService.class)
@ComponentScan("com.icthh.xm.commons.lep.spring")
@EnableAutoConfiguration
@Profile("resolveclasstest")
public class DynamicLepTestConfig extends LepSpringConfiguration {

    public DynamicLepTestConfig(final ApplicationEventPublisher eventPublisher,
                                final ResourceLoader resourceLoader) {
        super("testApp", eventPublisher, resourceLoader);
    }

    @Override
    protected TenantScriptStorage getTenantScriptStorageType() {
        return TenantScriptStorage.XM_MS_CONFIG;
    }

    @Bean
    public DynamicTestLepService testLepService() {
        return new DynamicTestLepService();
    }

    @Bean
    public LoggingConfigService LoggingConfigService() {
        return new LoggingConfigServiceStub();
    }

    @Bean
    public SpringLepProcessingApplicationListener springLepProcessingApplicationListener() {
        return new SpringLepProcessingApplicationListener() {
            @Override
            protected void bindExecutionContext(ScopedContext executionContext) {
            }
        };
    }

}
