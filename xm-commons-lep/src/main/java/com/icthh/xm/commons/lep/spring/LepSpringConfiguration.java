package com.icthh.xm.commons.lep.spring;

import static com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader.XM_MS_CONFIG_URL_PREFIX;
import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_SINGLETON;
import static org.springframework.core.io.ResourceLoader.CLASSPATH_URL_PREFIX;

import com.icthh.xm.commons.lep.RouterResourceLoader;
import com.icthh.xm.commons.lep.TenantScriptStorage;
import com.icthh.xm.commons.lep.XmExtensionService;
import com.icthh.xm.commons.lep.XmGroovyExecutionStrategy;
import com.icthh.xm.commons.lep.XmGroovyScriptEngineProviderStrategy;
import com.icthh.xm.commons.lep.XmLepResourceService;
import com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader;
import com.icthh.xm.lep.api.ExtensionService;
import com.icthh.xm.lep.api.LepExecutor;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.lep.api.LepResourceService;
import com.icthh.xm.lep.groovy.DefaultScriptNameLepResourceKeyMapper;
import com.icthh.xm.lep.groovy.ScriptNameLepResourceKeyMapper;
import com.icthh.xm.lep.groovy.StrategyGroovyLepExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.ResourceLoader;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * The {@link LepSpringConfiguration} class.
 */
@Configuration
public abstract class LepSpringConfiguration {

    private static final String FILE_URL_PREFIX = "file:";
    private static final int RESOURCE_LOADERS_CAPACITY = 3;

    private final String appName;
    private final ApplicationEventPublisher eventPublisher;
    private final ResourceLoader resourceLoader;
    @Autowired @Lazy
    private XmLepScriptConfigServerResourceLoader xmLepScriptConfigServerResourceLoader;

    protected LepSpringConfiguration(String appName,
                                     ApplicationEventPublisher eventPublisher,
                                     ResourceLoader resourceLoader) {
        this.appName = Objects.requireNonNull(appName);
        this.eventPublisher = eventPublisher;
        this.resourceLoader = resourceLoader;
    }

    @Bean
    @Scope(SCOPE_SINGLETON)
    protected LepManager lepManager() {
        return new SpringLepManager(extensionService(),
            lepExecutor(),
            applicationLepProcessingEventPublisher(),
            lepResourceService());
    }

    @Bean
    public ScriptNameLepResourceKeyMapper scriptNameLepResourceKeyMapper() {
        return new DefaultScriptNameLepResourceKeyMapper();
    }

    @Bean
    public XmGroovyScriptEngineProviderStrategy xmGroovyScriptEngineProviderStrategy() {
        return new XmGroovyScriptEngineProviderStrategy(scriptNameLepResourceKeyMapper());
    }

    @Bean
    public XmGroovyExecutionStrategy xmGroovyExecutionStrategy() {
        return new XmGroovyExecutionStrategy();
    }

    @Bean
    public LepExecutor lepExecutor() {
        return new StrategyGroovyLepExecutor(scriptNameLepResourceKeyMapper(),
                                             xmGroovyScriptEngineProviderStrategy(),
                                             xmGroovyExecutionStrategy());
    }

    @Bean
    public ExtensionService extensionService() {
        return new XmExtensionService();
    }

    @Bean
    public ApplicationLepProcessingEventPublisher applicationLepProcessingEventPublisher() {
        return new ApplicationLepProcessingEventPublisher(eventPublisher);
    }

    @Bean
    public XmLepScriptConfigServerResourceLoader cfgResourceLoader() {
        return new XmLepScriptConfigServerResourceLoader(appName);
    }

    @Bean
    public RouterResourceLoader routerResourceLoader() {
        Map<String, ResourceLoader> routerMap = new HashMap<>(RESOURCE_LOADERS_CAPACITY);
        routerMap.put(CLASSPATH_URL_PREFIX, resourceLoader);
        routerMap.put(XM_MS_CONFIG_URL_PREFIX, xmLepScriptConfigServerResourceLoader);
        routerMap.put(FILE_URL_PREFIX, new FileSystemResourceLoader());
        return new RouterResourceLoader(routerMap);
    }

    protected abstract TenantScriptStorage getTenantScriptStorageType();

    @Bean
    public LepResourceService lepResourceService() {
        return new XmLepResourceService(appName,
                                        getTenantScriptStorageType(),
                                        routerResourceLoader());
    }

}
