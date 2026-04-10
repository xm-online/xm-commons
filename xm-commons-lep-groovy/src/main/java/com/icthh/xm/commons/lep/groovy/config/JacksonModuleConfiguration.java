package com.icthh.xm.commons.lep.groovy.config;

import groovy.lang.GString;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.module.SimpleModule;

@Configuration
public class JacksonModuleConfiguration {

    @Bean
    public JacksonModule gStringModule(GStringJsonSerializer serializer) {
        SimpleModule module = new SimpleModule();
        module.addSerializer(GString.class, serializer);
        return module;
    }
}
