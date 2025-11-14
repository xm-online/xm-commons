package com.icthh.xm.commons.logging.configurable;

import com.codahale.metrics.MetricRegistry;
import com.icthh.xm.commons.config.client.repository.CommonConfigRepository;
import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import com.icthh.xm.commons.config.client.service.TenantAliasService;
import com.icthh.xm.commons.config.client.service.TenantAliasServiceImpl;
import com.icthh.xm.commons.lep.TenantScriptStorage;
import com.icthh.xm.commons.lep.groovy.GroovyLepEngineConfiguration;
import com.icthh.xm.commons.lep.spring.LepUpdateMode;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import static org.mockito.Mockito.mock;

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

    @Bean
    public MetricRegistry metricRegistry() {
        return new MetricRegistry();
    }

    @Override
    public LepUpdateMode lepUpdateMode() {
        return LepUpdateMode.SYNCHRONOUS;
    }


    @Bean
    public TenantAliasService tenantAliasService() {
        return new TenantAliasServiceImpl(mock(CommonConfigRepository.class), mock(TenantListRepository.class));
    }

}
