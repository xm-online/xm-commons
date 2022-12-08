package com.icthh.xm.commons.domainevent.outbox.config;

import com.icthh.xm.commons.domainevent.outbox.repository.OutboxRepository;
import com.icthh.xm.commons.migration.db.config.EntityScanPackageProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackageClasses = OutboxRepository.class)
public class OutboxDatabaseConfig implements EntityScanPackageProvider {

    private static final String JPA_PACKAGES = "com.icthh.xm.commons.domainevent.outbox.domain";

    @Override
    public String getJpaPackages() {
        return JPA_PACKAGES;
    }
}
