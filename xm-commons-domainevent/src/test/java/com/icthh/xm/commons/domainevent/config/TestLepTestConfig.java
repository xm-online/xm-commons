package com.icthh.xm.commons.domainevent.config;

import com.codahale.metrics.MetricRegistry;
import com.icthh.xm.commons.config.client.repository.CommonConfigRepository;
import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import com.icthh.xm.commons.config.client.service.TenantAliasService;
import com.icthh.xm.commons.config.client.service.TenantAliasServiceImpl;
import com.icthh.xm.commons.domainevent.service.filter.lep.WebLepFilter;
import com.icthh.xm.commons.domainevent.service.impl.KafkaTransactionSynchronizationAdapter;
import com.icthh.xm.commons.domainevent.service.impl.KafkaTransactionSynchronizationAdapterService;
import com.icthh.xm.commons.lep.groovy.GroovyLepEngineConfiguration;
import com.icthh.xm.commons.lep.spring.LepUpdateMode;
import com.icthh.xm.commons.logging.config.LoggingConfigService;
import com.icthh.xm.commons.logging.config.LoggingConfigServiceStub;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.SimpleTransactionScope;

import static org.mockito.Mockito.mock;

@Configuration
@ComponentScan({"com.icthh.xm.commons.lep.spring", "com.icthh.xm.commons.domainevent", "com.icthh.xm.commons.tenant"})
@EnableAutoConfiguration
public class TestLepTestConfig extends GroovyLepEngineConfiguration {

    public TestLepTestConfig(ConfigurableListableBeanFactory factory) {
        super("app-name");
        factory.registerScope("transaction", new SimpleTransactionScope());
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
    public MetricRegistry metricRegistry() {
        return new MetricRegistry();
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

    @Override
    public LepUpdateMode lepUpdateMode() {
        return LepUpdateMode.SYNCHRONOUS;
    }

    @Bean
    public TenantAliasService tenantAliasService() {
        return new TenantAliasServiceImpl(mock(CommonConfigRepository.class), mock(TenantListRepository.class));
    }

}
