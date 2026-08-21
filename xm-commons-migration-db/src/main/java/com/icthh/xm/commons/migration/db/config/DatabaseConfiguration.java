package com.icthh.xm.commons.migration.db.config;

import com.icthh.xm.commons.migration.db.XmMultiTenantSpringLiquibase;
import com.icthh.xm.commons.migration.db.XmSpringLiquibase;
import com.icthh.xm.commons.migration.db.tenant.SchemaResolver;
import liquibase.integration.spring.MultiTenantSpringLiquibase;
import liquibase.integration.spring.SpringLiquibase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.jpa.autoconfigure.JpaProperties;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import javax.sql.DataSource;
import java.util.Optional;

import static com.icthh.xm.commons.migration.db.Constants.CHANGE_LOG_PATH;
import static com.icthh.xm.commons.migration.db.util.CollectionsUtils.firstOrNull;

@Slf4j
public abstract class DatabaseConfiguration extends AbstractDatabaseConfiguration {

    private static final String SPRING_PROFILE_NO_LIQUIBASE = "no-liquibase";

    public DatabaseConfiguration(Environment env,
                                 JpaProperties jpaProperties,
                                 SchemaResolver schemaResolver) {
        super(env, jpaProperties, schemaResolver);
    }

    @Bean
    public SpringLiquibase liquibase(DataSource dataSource, LiquibaseProperties liquibaseProperties) {
        schemaResolver.createSchemas(dataSource);

        SpringLiquibase liquibase = new XmSpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog("classpath:config/liquibase/master.xml");
        liquibase.setContexts(firstOrNull(liquibaseProperties.getContexts()));
        liquibase.setDefaultSchema(liquibaseProperties.getDefaultSchema());
        liquibase.setDropFirst(liquibaseProperties.isDropFirst());
        if (env.acceptsProfiles(Profiles.of(SPRING_PROFILE_NO_LIQUIBASE))) {
            liquibase.setShouldRun(false);
        } else {
            liquibase.setShouldRun(liquibaseProperties.isEnabled());
            log.debug("Configuring Liquibase");
        }
        return liquibase;
    }

    @Bean
    @DependsOn("liquibase")
    public MultiTenantSpringLiquibase multiTenantLiquibase(DataSource dataSource,
                                                           LiquibaseProperties liquibaseProperties) {
        MultiTenantSpringLiquibase liquibase = new XmMultiTenantSpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog(CHANGE_LOG_PATH);
        liquibase.setContexts(firstOrNull(liquibaseProperties.getContexts()));
        liquibase.setDefaultSchema(liquibaseProperties.getDefaultSchema());
        liquibase.setDropFirst(liquibaseProperties.isDropFirst());
        liquibase.setSchemas(schemaResolver.getSchemas());
        if (env.acceptsProfiles(Profiles.of(SPRING_PROFILE_NO_LIQUIBASE))) {
            liquibase.setShouldRun(false);
        } else {
            liquibase.setShouldRun(liquibaseProperties.isEnabled());
            log.debug("Configuring Liquibase");
        }
        return liquibase;
    }

}

