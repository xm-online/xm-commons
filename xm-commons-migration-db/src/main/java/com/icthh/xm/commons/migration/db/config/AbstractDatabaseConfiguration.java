package com.icthh.xm.commons.migration.db.config;

import com.icthh.xm.commons.migration.db.tenant.SchemaResolver;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.boot.jpa.autoconfigure.JpaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
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

import static org.hibernate.cfg.AvailableSettings.JAKARTA_VALIDATION_FACTORY;
import static org.hibernate.cfg.AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER;
import static org.hibernate.cfg.AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER;

@EnableTransactionManagement
public abstract class AbstractDatabaseConfiguration {

    protected final Environment env;
    protected final JpaProperties jpaProperties;
    protected final SchemaResolver schemaResolver;

    public AbstractDatabaseConfiguration(Environment env,
                                         JpaProperties jpaProperties,
                                         SchemaResolver schemaResolver) {
        this.env = env;
        this.jpaProperties = jpaProperties;
        this.schemaResolver = schemaResolver;
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
        properties.put(MULTI_TENANT_CONNECTION_PROVIDER, multiTenantConnectionProviderImpl);
        properties.put(MULTI_TENANT_IDENTIFIER_RESOLVER, currentTenantIdentifierResolverImpl);
        properties.put(JAKARTA_VALIDATION_FACTORY, localValidatorFactoryBean);

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
        if (StringUtils.isNotBlank(jpaPackage)) {
            packageList.add(jpaPackage);
        }

        return packageList.toArray(String[]::new);
    }
}