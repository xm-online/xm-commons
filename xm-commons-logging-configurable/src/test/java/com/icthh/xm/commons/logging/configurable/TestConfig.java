package com.icthh.xm.commons.logging.configurable;

import com.icthh.xm.commons.lep.TenantScriptStorage;
import com.icthh.xm.commons.lep.groovy.GroovyLepEngineConfiguration;
import com.icthh.xm.commons.lep.spring.LepUpdateMode;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan("com.icthh.xm.commons.lep.spring")
@EnableAutoConfiguration
public class TestConfig extends GroovyLepEngineConfiguration {

    public TestConfig() {
        super("testApp");
    }

    @Override
    public TenantScriptStorage getTenantScriptStorageType() {
        return TenantScriptStorage.CLASSPATH;
    }

    @Bean
    public XmAuthenticationContextHolder authenticationContextHolder() {
        return Mockito.mock(XmAuthenticationContextHolder.class);
    }

    @Override
    public LepUpdateMode lepUpdateMode() {
        return LepUpdateMode.SYNCHRONOUS;
    }

}
