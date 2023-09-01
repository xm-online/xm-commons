package com.icthh.xm.commons.lep.spring;

import com.icthh.xm.commons.config.client.service.TenantAliasService;
import com.icthh.xm.commons.lep.RefreshTaskExecutor;
import com.icthh.xm.commons.lep.TenantScriptStorage;
import com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader;
import com.icthh.xm.commons.lep.api.LepManagementService;
import com.icthh.xm.commons.lep.groovy.GroovyLepEngineConfiguration;
import com.icthh.xm.commons.lep.groovy.GroovyLepEngineFactory;
import com.icthh.xm.commons.lep.groovy.TenantScriptStorageTypeProvider;
import com.icthh.xm.commons.lep.impl.utils.ClassPathLepRepository;
import com.icthh.xm.commons.logging.config.LoggingConfigService;
import com.icthh.xm.commons.logging.config.LoggingConfigServiceStub;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ResourceLoader;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;


@Configuration
@ComponentScan("com.icthh.xm.commons.lep.spring")
@EnableAutoConfiguration
@Profile({"!resolveclasstest && !resolvefiletest"})
public class LepTestConfig extends GroovyLepEngineConfiguration {

    public LepTestConfig() {
        super("testApp");
    }

    @Bean
    public TenantScriptStorageTypeProvider getTenantScriptStorageType() {
        return () -> TenantScriptStorage.CLASSPATH;
    }

    @Bean
    public TestLepService testLepService() {
        return new TestLepService();
    }

}
