package com.icthh.xm.commons.web.spring.config;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
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
     * Use Jackson2 because of Jackson 3 and newest version 3.0.0-rc2 has bugs
     */
    @Bean
    public JavaTimeModule javaTimeModule() {
        return new JavaTimeModule();
    }

    /*
     * Support for Hibernate types in Jackson.
     */

    @Bean
    public Hibernate7Module hibernate7Module() {
        return new Hibernate7Module();
    }

    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper objectMapper() {
        return JsonMapper.builder().build();
    }


    @Bean
    public JsonMapperBuilderCustomizer jsonMapperBuilderCustomizer(AutowireCapableBeanFactory beanFactory) {
        return builder -> builder.handlerInstantiator(new JacksonHandlerInstantiator(beanFactory));
    }

}
