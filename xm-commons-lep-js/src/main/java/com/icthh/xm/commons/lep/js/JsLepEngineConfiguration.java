package com.icthh.xm.commons.lep.js;

import com.icthh.xm.commons.lep.spring.ApplicationNameProvider;
import com.icthh.xm.commons.logging.config.LoggingConfigService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnMissingBean(JsLepEngineConfiguration.class)
public class JsLepEngineConfiguration {

    @Bean
    public JsLepEngineFactory jsLepEngineFactory(ApplicationNameProvider applicationNameProvider,
                                                 LoggingWrapper loggingWrapper) {
        String appName = applicationNameProvider.getAppName();
        return new JsLepEngineFactory(
            appName,
            loggingWrapper
        );
    }

    @Bean
    @ConditionalOnMissingBean(LoggingWrapper.class)
    public LoggingWrapper loggingWrapper(LoggingConfigService loggingConfigService) {
        return new LoggingWrapper(loggingConfigService);
    }

}
