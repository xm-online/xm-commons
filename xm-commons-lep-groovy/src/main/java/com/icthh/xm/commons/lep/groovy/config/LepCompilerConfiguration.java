package com.icthh.xm.commons.lep.groovy.config;

import com.icthh.xm.commons.config.client.service.TenantAliasPreCompileServiceImpl;
import com.icthh.xm.commons.config.client.service.TenantAliasService;
import com.icthh.xm.commons.lep.LepPathResolver;
import com.icthh.xm.commons.lep.groovy.GroovyEngineCreationStrategy;
import com.icthh.xm.commons.lep.groovy.GroovyFileParser;
import com.icthh.xm.commons.lep.groovy.RecreateGroovyLepEngineOnRefresh;
import com.icthh.xm.commons.lep.groovy.storage.LepStorageFactory;
import com.icthh.xm.commons.lep.groovy.storage.XmConfigLepStorageFactory;
import com.icthh.xm.commons.lep.impl.LoggingWrapper;
import com.icthh.xm.commons.lep.impl.utils.ClassPathLepRepository;
import com.icthh.xm.commons.lep.spring.ApplicationNameProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;


@Configuration
@Profile("export")
public class LepCompilerConfiguration {

    @Value("${spring.application.name:}")
    private String appName;

    @Bean
    public ApplicationNameProvider applicationNameProvider() {
        return new ApplicationNameProvider(appName);
    }

    @Bean
    public TenantAliasService tenantAliasService() {
        return new TenantAliasPreCompileServiceImpl();
    }

    @Bean
    public LepPathResolver lepPathResolver(ApplicationNameProvider applicationNameProvider,
                                           TenantAliasService tenantAliasService) {
        return new LepPathResolver(applicationNameProvider, tenantAliasService);
    }

    @Bean
    public ClassPathLepRepository classPathLepRepository(ApplicationContext applicationContext) {
        return new ClassPathLepRepository(applicationContext);
    }

    @Bean
    public LepStorageFactory lepStorageFactory(ApplicationNameProvider applicationNameProvider,
                                               ClassPathLepRepository classPathLepRepository) {
        return new XmConfigLepStorageFactory(applicationNameProvider.getAppName(), classPathLepRepository);
    }

    @Bean
    public GroovyEngineCreationStrategy groovyEngineCreationStrategy() {
        return new RecreateGroovyLepEngineOnRefresh();
    }

    @Bean
    public GroovyFileParser groovyFileParser() {
        return new GroovyFileParser();
    }

    @Bean
    public LoggingWrapper loggingWrapper() {
        return new LoggingWrapper(null);
    }
}
