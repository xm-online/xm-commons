package com.icthh.xm.commons.domainevent.service.imp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.icthh.xm.commons.domainevent.config.KafkaTransactionalConfig;
import com.icthh.xm.commons.domainevent.domain.DomainEvent;
import com.icthh.xm.commons.domainevent.domain.enums.DefaultDomainEventSource;
import com.icthh.xm.commons.topic.service.KafkaTemplateService;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.Properties;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = {
    ObjectMapper.class,
    KafkaTransactionSynchronizationAdapter.class,
    KafkaTransactionalConfig.class,
    TestTransactionalProxyService.class,
    KafkaTemplateService.class,
    SyncKafkaTransport.class,
    SyncKafkaTransportIntTest.H2TestProfileJPAConfig.class
})
@RunWith(SpringJUnit4ClassRunner.class)
public class SyncKafkaTransportIntTest {

    private static final String TENANT = "TEST_TENANT";
    private static final String SOURCE = DefaultDomainEventSource.WEB.getCode();

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestTransactionalProxyService testTransactionalProxyService;

    @MockBean
    private KafkaTemplateService kafkaTemplateService;


    @Before
    public void before() {
        MockitoAnnotations.openMocks(this);
        objectMapper.registerModules(new JavaTimeModule());
    }

    @Test
    public void sendWithoutTransactional() {

        DomainEvent domainEvent = new DomainEvent();
        domainEvent.setId(UUID.randomUUID());
        domainEvent.setTenant(TENANT);
        domainEvent.setSource(SOURCE);

        testTransactionalProxyService.send(domainEvent);

        String topic = String.format("event.%s.%s", TENANT, SOURCE).toLowerCase();
        verify(kafkaTemplateService, times(1)).send(topic, toJson(domainEvent));
    }

    @Test
    public void sendInTransactional_shouldSend() {
        DomainEvent domainEvent = new DomainEvent();
        domainEvent.setId(UUID.randomUUID());
        domainEvent.setTenant(TENANT);
        domainEvent.setSource(SOURCE);

        String json = toJson(domainEvent);
        String topic = String.format("event.%s.%s", TENANT, SOURCE).toLowerCase();

        testTransactionalProxyService.sendTransactional(domainEvent);
        verify(kafkaTemplateService, times(2)).send(topic, json);
    }

    @Test
    public void sendInTransactional_withError_shouldRollback() {
        DomainEvent domainEvent = new DomainEvent();
        domainEvent.setId(UUID.randomUUID());
        domainEvent.setTenant(TENANT);
        domainEvent.setSource(SOURCE);

        String json = toJson(domainEvent);
        String topic = String.format("event.%s.%s", TENANT, SOURCE).toLowerCase();

        verify(kafkaTemplateService, times(0)).send(topic, json);
        try {
            testTransactionalProxyService.sendTransactionalWithError(domainEvent);
        } catch (IllegalArgumentException e) {
            assertEquals("Some error!", e.getMessage());
        } finally {
            verify(kafkaTemplateService, times(0)).send(topic, json);
        }
    }

    @SneakyThrows
    private String toJson(DomainEvent domainEvent) {
        return objectMapper.writeValueAsString(domainEvent);
    }

    @TestConfiguration
    @EnableTransactionManagement
    public static class H2TestProfileJPAConfig {

        @Bean
        public DataSource dataSource() {
            final DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setDriverClassName("org.h2.Driver");
            dataSource.setUrl("jdbc:h2:mem:db;DB_CLOSE_DELAY=-1");
            dataSource.setUsername("sa");
            dataSource.setPassword("sa");

            return dataSource;
        }

        @Bean
        public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
            final LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
            em.setDataSource(dataSource());
            em.setPackagesToScan(new String[]{"com.icthh.xm.commons.domainevent"});
            em.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            em.setJpaProperties(additionalProperties());
            return em;
        }

        @Bean
        JpaTransactionManager transactionManager(final EntityManagerFactory entityManagerFactory) {
            final JpaTransactionManager transactionManager = new JpaTransactionManager();
            transactionManager.setEntityManagerFactory(entityManagerFactory);
            return transactionManager;
        }

        final Properties additionalProperties() {
            final Properties hibernateProperties = new Properties();

            hibernateProperties.setProperty("hibernate.hbm2ddl.auto", "create-drop");
            hibernateProperties.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
            hibernateProperties.setProperty("hibernate.show_sql", "true");
            hibernateProperties.setProperty("hibernate.globally_quoted_identifiers", "true");

            return hibernateProperties;
        }
    }

}
