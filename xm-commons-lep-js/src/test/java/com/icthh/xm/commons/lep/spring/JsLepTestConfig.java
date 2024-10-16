package com.icthh.xm.commons.lep.spring;

import com.icthh.xm.commons.config.client.repository.CommonConfigRepository;
import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import com.icthh.xm.commons.config.client.service.TenantAliasService;
import com.icthh.xm.commons.config.client.service.TenantAliasServiceImpl;
import com.icthh.xm.commons.lep.impl.LoggingWrapper;
import com.icthh.xm.commons.lep.js.JsLepEngineFactory;
import com.icthh.xm.commons.logging.config.LoggingConfigService;
import com.icthh.xm.commons.logging.config.LoggingConfigServiceStub;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.mockito.Mockito.mock;

/**
 *
 */
@Configuration
@Import({LepSpringConfiguration.class})
public class JsLepTestConfig {

    public JsLepTestConfig() {
    }

    @Bean
    public JsLepEngineFactory jsLepEngineFactory(LoggingWrapper loggingWrapper, ApplicationNameProvider applicationNameProvider) {
        return new JsLepEngineFactory(applicationNameProvider, loggingWrapper);
    }

    @Bean
    public LepUpdateMode lepUpdateMode() {
        return LepUpdateMode.SYNCHRONOUS;
    }

    @Bean
    public JsTestLepService testLepService() {
        return new JsTestLepService();
    }

    @Bean
    public JsTestResolver jsTestResolver() {
        return new JsTestResolver();
    }

    @Bean
    public LoggingConfigService LoggingConfigService() {
        return new LoggingConfigServiceStub();
    }

    @Bean
    public TenantAliasService tenantAliasService() {
        return new TenantAliasServiceImpl(mock(CommonConfigRepository.class), mock(TenantListRepository.class));
    }

}
