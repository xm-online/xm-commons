package com.icthh.xm.commons.web.spring.config;

import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
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
    public Hibernate6Module hibernate6Module() {
        return new Hibernate6Module();
    }

    @Bean
    public JsonMapperBuilderCustomizer jsonMapperBuilderCustomizer(AutowireCapableBeanFactory beanFactory) {
        return builder -> builder.handlerInstantiator(new JacksonHandlerInstantiator(beanFactory));
    }

}
