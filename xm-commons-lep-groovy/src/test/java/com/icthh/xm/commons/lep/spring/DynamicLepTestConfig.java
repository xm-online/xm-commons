package com.icthh.xm.commons.lep.spring;

import com.icthh.xm.commons.config.client.repository.CommonConfigRepository;
import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import com.icthh.xm.commons.config.client.service.TenantAliasService;
import com.icthh.xm.commons.config.client.service.TenantAliasServiceImpl;
import com.icthh.xm.commons.lep.api.LepContextFactory;
import com.icthh.xm.commons.lep.groovy.GroovyLepEngineConfiguration;
import com.icthh.xm.commons.logging.config.LoggingConfigService;
import com.icthh.xm.commons.logging.config.LoggingConfigServiceStub;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static org.mockito.Mockito.mock;

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

    @Bean
    public LoggingConfigService LoggingConfigService() {
        return new LoggingConfigServiceStub();
    }

    @Bean
    public LepContextFactory lepContextFactory() {
        return lepMethod -> new LepContext();
    }

    @Bean
    public TenantAliasService tenantAliasService() {
        return new TenantAliasServiceImpl(mock(CommonConfigRepository.class), mock(TenantListRepository.class));
    }

}
