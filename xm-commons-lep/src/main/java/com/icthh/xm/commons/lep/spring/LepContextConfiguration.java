package com.icthh.xm.commons.lep.spring;

import com.icthh.xm.commons.lep.api.BaseLepContext;
import com.icthh.xm.commons.lep.api.LepContextFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LepContextConfiguration {

    @Bean
    @ConditionalOnMissingBean(LepContextFactory.class)
    public LepContextFactory lepContextFactory() {
        return lepMethod -> new BaseLepContext() {};
    }

}
