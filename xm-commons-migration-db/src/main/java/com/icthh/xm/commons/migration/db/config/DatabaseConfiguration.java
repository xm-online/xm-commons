package com.icthh.xm.commons.migration.db.config;

import com.icthh.xm.commons.migration.db.XmMultiTenantSpringLiquibase;
import com.icthh.xm.commons.migration.db.XmSpringLiquibase;
import com.icthh.xm.commons.migration.db.tenant.SchemaResolver;
import liquibase.integration.spring.MultiTenantSpringLiquibase;
import liquibase.integration.spring.SpringLiquibase;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.MultiTenancyStrategy;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.icthh.xm.commons.migration.db.Constants.CHANGE_LOG_PATH;
import static org.hibernate.cfg.AvailableSettings.JPA_VALIDATION_FACTORY;
import static org.hibernate.cfg.AvailableSettings.MULTI_TENANT;
import static org.hibernate.cfg.AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER;
import static org.hibernate.cfg.AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER;

@Slf4j
@EnableTransactionManagement
public abstract class DatabaseConfiguration {

    private static final String SPRING_PROFILE_NO_LIQUIBASE = "no-liquibase";

    private final Environment env;
    private final JpaProperties jpaProperties;
    private final SchemaResolver schemaResolver;

    public DatabaseConfiguration(Environment env,
                                 JpaProperties jpaProperties,
                                 SchemaResolver schemaResolver) {
        this.env = env;
        this.jpaProperties = jpaProperties;
        this.schemaResolver = schemaResolver;
    }

    @Bean
    public SpringLiquibase liquibase(DataSource dataSource, LiquibaseProperties liquibaseProperties) {
        schemaResolver.createSchemas(dataSource);

        SpringLiquibase liquibase = new XmSpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog("classpath:config/liquibase/master.xml");
        liquibase.setContexts(liquibaseProperties.getContexts());
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
        liquibase.setContexts(liquibaseProperties.getContexts());
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

    @Bean
    public JpaVendorAdapter jpaVendorAdapter() {
        return new HibernateJpaVendorAdapter();
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            DataSource dataSource,
            MultiTenantConnectionProvider multiTenantConnectionProviderImpl,
            CurrentTenantIdentifierResolver currentTenantIdentifierResolverImpl,
            LocalValidatorFactoryBean localValidatorFactoryBean,
            List<EntityScanPackageProvider> entityScanPackageProviderList) {

        Map<String, Object> properties = new HashMap<>(jpaProperties.getProperties());
        properties.put(MULTI_TENANT, MultiTenancyStrategy.SCHEMA);
        properties.put(MULTI_TENANT_CONNECTION_PROVIDER, multiTenantConnectionProviderImpl);
        properties.put(MULTI_TENANT_IDENTIFIER_RESOLVER, currentTenantIdentifierResolverImpl);
        properties.put(JPA_VALIDATION_FACTORY, localValidatorFactoryBean);

        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan(getJpaPackages(entityScanPackageProviderList));
        em.setJpaVendorAdapter(jpaVendorAdapter());
        em.setJpaPropertyMap(properties);
        return em;
    }

    public abstract String getJpaPackages();

    private String[] getJpaPackages(List<EntityScanPackageProvider> entityScanPackageProviderList) {
        List<String> packageList = entityScanPackageProviderList.stream()
            .filter(it -> it != null && StringUtils.isNotBlank(it.getJpaPackages()))
            .map(EntityScanPackageProvider::getJpaPackages)
            .collect(Collectors.toList());

        String jpaPackage = getJpaPackages();
        if (StringUtils.isNotBlank(jpaPackage)){
            packageList.add(jpaPackage);
        }

        return packageList.toArray(String[]::new);
    }
}

