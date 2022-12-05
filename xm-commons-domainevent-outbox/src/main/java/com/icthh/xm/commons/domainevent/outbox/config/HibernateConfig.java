package com.icthh.xm.commons.domainevent.outbox.config;

import com.icthh.xm.commons.domainevent.outbox.domain.Outbox;
import com.icthh.xm.commons.domainevent.outbox.repository.OutboxRepository;
import org.hibernate.MultiTenancyStrategy;
import org.hibernate.cfg.Environment;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.domain.EntityScanPackages;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@EnableJpaRepositories(basePackageClasses = OutboxRepository.class)
public class HibernateConfig {

    @Autowired
    private JpaProperties jpaProperties;

    @Autowired
    private BeanFactory beanFactory;

    @Bean
    JpaVendorAdapter jpaVendorAdapter() {
        return new HibernateJpaVendorAdapter();
    }

    //TODO In commons v3.0 this config should be already in commons (not in any microservice)
    //should not forget to use one from commons v3.
    @Bean
    @Primary
    LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource,
            MultiTenantConnectionProvider multiTenantConnectionProvider,
            CurrentTenantIdentifierResolver currentTenantIdentifierResolver) {

        Map<String, Object> jpaPropertiesMap = new HashMap<>(jpaProperties.getProperties());
        jpaPropertiesMap.put(Environment.MULTI_TENANT, MultiTenancyStrategy.SCHEMA);
        jpaPropertiesMap.put(Environment.MULTI_TENANT_CONNECTION_PROVIDER, multiTenantConnectionProvider);
        jpaPropertiesMap.put(Environment.MULTI_TENANT_IDENTIFIER_RESOLVER, currentTenantIdentifierResolver);

        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan(getPackagesToScan());
        em.setJpaVendorAdapter(this.jpaVendorAdapter());
        em.setJpaPropertyMap(jpaPropertiesMap);
        return em;
    }

    private String[] getPackagesToScan() {
        ArrayList<String> packagesToScan = new ArrayList<>(Arrays.asList(getEntityScanPackages()));
        packagesToScan.add(Outbox.class.getPackageName());
        return packagesToScan.toArray(new String[0]);
    }

    protected String[] getEntityScanPackages() {
        List<String> packages = EntityScanPackages.get(this.beanFactory).getPackageNames();
        if (packages.isEmpty() && AutoConfigurationPackages.has(this.beanFactory)) {
            packages = AutoConfigurationPackages.get(this.beanFactory);
        }
        return StringUtils.toStringArray(packages);
    }
}
