package com.icthh.xm.commons.lep.spring;

import com.icthh.xm.commons.config.client.service.TenantAliasService;
import com.icthh.xm.commons.lep.RefreshTaskExecutor;
import com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader;
import com.icthh.xm.commons.lep.api.LepManagementService;
import com.icthh.xm.commons.lep.groovy.GroovyLepEngineFactory;
import com.icthh.xm.commons.lep.impl.utils.ClassPathLepRepository;
import com.icthh.xm.commons.logging.config.LoggingConfigService;
import com.icthh.xm.commons.logging.config.LoggingConfigServiceStub;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ResourceLoader;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

/**
 *
 */
@Configuration
@ComponentScan("com.icthh.xm.commons.lep.spring")
@EnableAutoConfiguration
@Profile({"!resolveclasstest && !resolvefiletest"})
public class LepTestConfig extends LepSpringConfiguration {

    public LepTestConfig(final ApplicationEventPublisher eventPublisher,
                         final ResourceLoader resourceLoader) {
        super("testApp");
    }

    //@Override
    //protected TenantScriptStorage getTenantScriptStorageType() {
    //    return TenantScriptStorage.CLASSPATH;
    //}

    @Bean
    public TestLepService testLepService() {
        return new TestLepService();
    }

    @Bean
    public LoggingConfigService LoggingConfigService() {
        return new LoggingConfigServiceStub();
    }

    @Bean
    public RefreshTaskExecutor refreshTaskExecutor() {
        return new RefreshTaskExecutor() {
            CallerRunsPolicy callerRunsPolicy = new ThreadPoolExecutor.CallerRunsPolicy();
            @Override
            public void execute(Runnable command) {
                callerRunsPolicy.rejectedExecution(command, this);
            }
        };
    }

    @Bean
    public XmLepScriptConfigServerResourceLoader cfgResourceLoader(LepManagementService lepManagementService,
                                                                   RefreshTaskExecutor refreshTaskExecutor) {
        var resourceLoader = new XmLepScriptConfigServerResourceLoader("testApp", lepManagementService, refreshTaskExecutor) {
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
        return new GroovyLepEngineFactory("testApp", classPathLepRepository, tenantAliasService);
    }

}
