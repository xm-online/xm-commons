package com.icthh.xm.commons.lep.spring;

import com.icthh.xm.commons.lep.TenantScriptStorage;
import com.icthh.xm.commons.lep.spring.TestLepService;
import com.icthh.xm.commons.lep.spring.EnableLepServices;
import com.icthh.xm.commons.lep.spring.LepSpringConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

/**
 *
 */
@Configuration
@EnableLepServices(basePackageClasses = TestLepService.class)
@ComponentScan("com.icthh.xm.commons.lep.spring")
@EnableAutoConfiguration
public class LepTestConfig extends LepSpringConfiguration {

    public LepTestConfig(final ApplicationEventPublisher eventPublisher,
                         final ResourceLoader resourceLoader) {
        super("testApp", eventPublisher, resourceLoader);
    }

    @Override
    protected TenantScriptStorage getTenantScriptStorageType() {
        return TenantScriptStorage.CLASSPATH;
    }

    @Bean
    public TestLepService testLepService() {
        return new TestLepService();
    }

}
