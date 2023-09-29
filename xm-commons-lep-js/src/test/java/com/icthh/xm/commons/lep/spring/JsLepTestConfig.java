package com.icthh.xm.commons.lep.spring;

import com.icthh.xm.commons.lep.js.JsLepEngineConfiguration;
import com.icthh.xm.commons.logging.config.LoggingConfigService;
import com.icthh.xm.commons.logging.config.LoggingConfigServiceStub;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

/**
 *
 */
@Configuration
@Import({LepSpringConfiguration.class, JsLepEngineConfiguration.class})
public class JsLepTestConfig {

    public JsLepTestConfig() {
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
    public LoggingConfigService LoggingConfigService() {
        return new LoggingConfigServiceStub();
    }



}
