package com.icthh.xm.commons.domainevent.configuration;

import com.icthh.xm.commons.domainevent.service.builder.DomainEventFactory;
import com.icthh.xm.commons.domainevent.service.db.impl.TypeKeyAwareJpaEntityMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JpaEntityMapperConfiguration {

    @Bean
    TypeKeyAwareJpaEntityMapper typeKeyAwareJpaEntityMapper(DomainEventFactory domainEventFactory){
        return new TypeKeyAwareJpaEntityMapper(domainEventFactory);
    }

}
