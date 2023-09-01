package com.icthh.xm.commons.lep.spring;

import com.icthh.xm.commons.lep.groovy.GroovyLepEngineConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 *
 */
@Configuration
@EnableAutoConfiguration
@Profile("resolveclasstest")
public class DynamicLepTestConfig extends GroovyLepEngineConfiguration {

    public DynamicLepTestConfig() {
        super("testApp");
    }

    @Bean
    public DynamicTestLepService testLepService() {
        return new DynamicTestLepService();
    }

    @Override
    public LepUpdateMode lepUpdateMode() {
        return LepUpdateMode.SYNCHRONOUS;
    }
}
