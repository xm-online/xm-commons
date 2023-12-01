package com.icthh.xm.commons.lep.commons;

import org.springframework.context.annotation.Bean;

public class CommonsConfiguration {

    @Bean
    public CommonsService commonsService() {
        return new CommonsService();
    }

    @Bean
    public CommonsLepResolver commonsLepResolver() {
        return new CommonsLepResolver();
    }

}
