package com.icthh.xm.commons.lep.spring;

import com.icthh.xm.commons.lep.TenantScriptStorage;
import com.icthh.xm.commons.lep.groovy.GroovyLepEngineConfiguration;
import com.icthh.xm.commons.lep.groovy.TenantScriptStorageTypeProvider;
import com.icthh.xm.commons.logging.config.LoggingConfigService;
import com.icthh.xm.commons.logging.config.LoggingConfigServiceStub;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;


@Configuration
@ComponentScan("com.icthh.xm.commons.lep.spring")
@EnableAutoConfiguration
@Profile({"!resolveclasstest && !resolvefiletest"})
public class LepTestConfig extends GroovyLepEngineConfiguration {

    public LepTestConfig() {
        super("testApp");
    }

    @Bean
    public TenantScriptStorageTypeProvider getTenantScriptStorageType() {
        return () -> TenantScriptStorage.CLASSPATH;
    }

    @Bean
    public TestLepService testLepService() {
        return new TestLepService();
    }

    @Bean
    public LoggingConfigService LoggingConfigService() {
        return new LoggingConfigServiceStub();
    }

}
