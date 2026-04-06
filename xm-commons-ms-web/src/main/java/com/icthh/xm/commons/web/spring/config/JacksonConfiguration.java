package com.icthh.xm.commons.web.spring.config;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.icthh.xm.commons.lep.groovy.config.GStringJsonSerializer;
import groovy.lang.GString;
import org.springframework.context.annotation.Primary;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.datatype.hibernate7.Hibernate7Module;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.support.JacksonHandlerInstantiator;

@Configuration
public class JacksonConfiguration {

    /**
     * Support for Java date and time API.
     * @return the corresponding Jackson module.
     */
    @Bean
    public JavaTimeModule javaTimeModule() {
        return new JavaTimeModule();
    }

    /*
     * Support for Hibernate types in Jackson.
     */

    @Bean
    public JacksonModule gStringModule(GStringJsonSerializer serializer) {
        SimpleModule module = new SimpleModule();
        module.addSerializer(GString.class, serializer);
        return module;
    }

    @Bean
    public Hibernate7Module hibernate7Module() {
        return new Hibernate7Module();
    }

    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper objectMapper(JacksonModule gStringModule) {
        return JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .addModules(gStringModule)
                .build();
    }


    @Bean
    public JsonMapperBuilderCustomizer jsonMapperBuilderCustomizer(AutowireCapableBeanFactory beanFactory) {
        return builder -> builder.handlerInstantiator(new JacksonHandlerInstantiator(beanFactory));
    }

}
