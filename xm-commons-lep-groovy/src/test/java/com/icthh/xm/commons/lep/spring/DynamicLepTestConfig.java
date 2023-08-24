package com.icthh.xm.commons.lep.spring;

import com.icthh.xm.commons.logging.config.LoggingConfigService;
import com.icthh.xm.commons.logging.config.LoggingConfigServiceStub;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.TestPropertySource;

/**
 *
 */
@Configuration
@ComponentScan("com.icthh.xm.commons.lep")
@EnableAutoConfiguration
@Profile("resolveclasstest")
public class DynamicLepTestConfig extends LepSpringConfiguration {

    public DynamicLepTestConfig() {
        super("testApp");
    }

    @Bean
    public DynamicTestLepService testLepService() {
        return new DynamicTestLepService();
    }

    @Bean
    public LoggingConfigService LoggingConfigService() {
        return new LoggingConfigServiceStub();
    }

}
