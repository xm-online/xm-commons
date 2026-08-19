package com.icthh.xm.commons.migration.db.flyway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Registers LiquibaseProperties bean without enabling LiquibaseAutoConfiguration.
 */
@Configuration
@EnableConfigurationProperties(LiquibaseProperties.class)
public class LiquibaseConfiguration {
}
