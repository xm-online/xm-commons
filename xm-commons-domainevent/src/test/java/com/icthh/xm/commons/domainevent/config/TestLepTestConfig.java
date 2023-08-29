package com.icthh.xm.commons.domainevent.config;

import com.icthh.xm.commons.config.client.service.TenantAliasService;
import com.icthh.xm.commons.domainevent.service.filter.lep.WebLepFilter;
import com.icthh.xm.commons.domainevent.service.impl.KafkaTransactionSynchronizationAdapter;
import com.icthh.xm.commons.domainevent.service.impl.KafkaTransactionSynchronizationAdapterService;
import com.icthh.xm.commons.lep.RefreshTaskExecutor;
import com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader;
import com.icthh.xm.commons.lep.api.LepManagementService;
import com.icthh.xm.commons.lep.groovy.GroovyLepEngineFactory;
import com.icthh.xm.commons.lep.spring.LepSpringConfiguration;
import com.icthh.xm.commons.lep.impl.utils.ClassPathLepRepository;
import com.icthh.xm.commons.logging.config.LoggingConfigService;
import com.icthh.xm.commons.logging.config.LoggingConfigServiceStub;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.SimpleTransactionScope;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

/**
 *
 */
@Configuration
@ComponentScan({"com.icthh.xm.commons.lep.spring", "com.icthh.xm.commons.domainevent", "com.icthh.xm.commons.tenant"})
@EnableAutoConfiguration
public class TestLepTestConfig extends LepSpringConfiguration {

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
    KafkaTransactionSynchronizationAdapterService kafkaTransactionSynchronizationAdapterService(ApplicationContext context) {
        return new KafkaTransactionSynchronizationAdapterService() {
            @Override
            public KafkaTransactionSynchronizationAdapter getKafkaTransactionSynchronizationAdapter() {
                return context.getBean(KafkaTransactionSynchronizationAdapter.class);
            }
        };
    }

    @Bean
    public RefreshTaskExecutor refreshTaskExecutor() {
        return new RefreshTaskExecutor() {
            ThreadPoolExecutor.CallerRunsPolicy callerRunsPolicy = new ThreadPoolExecutor.CallerRunsPolicy();
            @Override
            public void execute(Runnable command) {
                callerRunsPolicy.rejectedExecution(command, this);
            }
        };
    }

    @Bean
    public XmLepScriptConfigServerResourceLoader cfgResourceLoader(LepManagementService lepManagementService,
                                                                   RefreshTaskExecutor refreshTaskExecutor) {
        var resourceLoader = new XmLepScriptConfigServerResourceLoader("app-name", lepManagementService, refreshTaskExecutor) {
            @Override
            public void onRefresh(String updatedKey, String configContent) {
                super.onRefresh(updatedKey, configContent);
                refreshFinished(List.of(updatedKey));
            }
        };
        resourceLoader.refreshFinished(List.of());
        return resourceLoader;
    }

    @Bean
    public GroovyLepEngineFactory groovyLepEngineFactory(ClassPathLepRepository classPathLepRepository,
                                                         TenantAliasService tenantAliasService) {
        return new GroovyLepEngineFactory("app-name", classPathLepRepository, tenantAliasService);
    }

}
