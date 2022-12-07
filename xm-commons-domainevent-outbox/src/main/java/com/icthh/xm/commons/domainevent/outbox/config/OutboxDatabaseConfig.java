package com.icthh.xm.commons.domainevent.outbox.config;

import com.icthh.xm.commons.domainevent.outbox.repository.OutboxRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackageClasses = OutboxRepository.class)
public class OutboxDatabaseConfig {

}
