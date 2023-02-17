package com.icthh.xm.commons.domainevent.config;

import com.icthh.xm.commons.domainevent.service.filter.lep.WebLepFilter;
import com.icthh.xm.commons.domainevent.service.filter.lep.WebLepFilterIntTest;
import com.icthh.xm.commons.domainevent.service.impl.KafkaTransactionSynchronizationAdapter;
import com.icthh.xm.commons.domainevent.service.impl.KafkaTransactionSynchronizationAdapterService;
import com.icthh.xm.commons.lep.TenantScriptStorage;
import com.icthh.xm.commons.lep.spring.EnableLepServices;
import com.icthh.xm.commons.lep.spring.SpringLepProcessingApplicationListener;
import com.icthh.xm.commons.lep.spring.lepservice.LepServiceFactory;
import com.icthh.xm.commons.lep.spring.web.WebLepSpringConfiguration;
import com.icthh.xm.commons.logging.config.LoggingConfigService;
import com.icthh.xm.commons.logging.config.LoggingConfigServiceStub;
import com.icthh.xm.lep.api.ScopedContext;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.transaction.support.SimpleTransactionScope;

/**
 *
 */
@Configuration
@EnableLepServices(basePackageClasses = {WebLepFilterIntTest.class, LepServiceFactory.class})
@ComponentScan({"com.icthh.xm.commons.lep.spring", "com.icthh.xm.commons.domainevent", "com.icthh.xm.commons.tenant"})
@EnableAutoConfiguration
public class TestLepTestConfig extends WebLepSpringConfiguration {

    public TestLepTestConfig(ApplicationEventPublisher eventPublisher,
                             ResourceLoader resourceLoader,
                             ConfigurableListableBeanFactory factory) {
        super("app-name", eventPublisher, resourceLoader);
        factory.registerScope("transaction", new SimpleTransactionScope());
    }

    @Override
    protected TenantScriptStorage getTenantScriptStorageType() {
        return TenantScriptStorage.XM_MS_CONFIG;
    }

    @Bean
    public LoggingConfigService LoggingConfigService() {
        return new LoggingConfigServiceStub();
    }

    @Bean
    public WebLepFilter webLepFilter() {
        return new WebLepFilter();
    }

    @Bean
    public SpringLepProcessingApplicationListener springLepProcessingApplicationListener() {
        return new SpringLepProcessingApplicationListener() {
            @Override
            protected void bindExecutionContext(ScopedContext executionContext) {
            }
        };
    }

    @Bean
    KafkaTransactionSynchronizationAdapterService kafkaTransactionSynchronizationAdapterService(ApplicationContext context) {
        return new KafkaTransactionSynchronizationAdapterService() {
            @Override
            public KafkaTransactionSynchronizationAdapter getKafkaTransactionSynchronizationAdapter() {
                return context.getBean(KafkaTransactionSynchronizationAdapter.class);
            }
        };
    }

}
