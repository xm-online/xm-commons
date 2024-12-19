package com.icthh.xm.commons.config;

import com.icthh.xm.commons.lep.keyresolver.FunctionLepKeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The {@link FunctionLepConfiguration} class.
 */
@Configuration
public class FunctionLepConfiguration {

    @Bean
    public FunctionLepKeyResolver functionLepKeyResolver() {
        return new FunctionLepKeyResolver();
    }
}
