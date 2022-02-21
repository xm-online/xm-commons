package com.icthh.xm.commons.lep.spring;

import static com.icthh.xm.commons.lep.TenantScriptStorage.CLASSPATH;
import static com.icthh.xm.commons.lep.TenantScriptStorage.FILE;
import static com.icthh.xm.commons.lep.TenantScriptStorage.XM_MS_CONFIG;
import static com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader.XM_MS_CONFIG_URL_PREFIX;
import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_SINGLETON;
import static org.springframework.core.io.ResourceLoader.CLASSPATH_URL_PREFIX;

import com.icthh.xm.commons.config.client.service.TenantAliasService;
import com.icthh.xm.commons.lep.CacheableLepEngine;
import com.icthh.xm.commons.lep.FileSystemUtils;
import com.icthh.xm.commons.lep.RouterResourceLoader;
import com.icthh.xm.commons.lep.TenantScriptStorage;
import com.icthh.xm.commons.lep.XmExtensionService;
import com.icthh.xm.commons.lep.XmFileSystemResourceLoader;
import com.icthh.xm.commons.lep.XmGroovyExecutionStrategy;
import com.icthh.xm.commons.lep.XmGroovyScriptEngineProviderStrategy;
import com.icthh.xm.commons.lep.XmLepResourceService;
import com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader;
import com.icthh.xm.commons.lep.storage.ClassPathTenantScriptPathResolver;
import com.icthh.xm.commons.lep.storage.FileTenantScriptPathResolver;
import com.icthh.xm.commons.lep.storage.TenantScriptPathResolver;
import com.icthh.xm.commons.lep.storage.XmMsConfigTenantScriptPathResolver;
import com.icthh.xm.commons.logging.config.LoggingConfigService;
import com.icthh.xm.lep.api.ExtensionService;
import com.icthh.xm.lep.api.LepExecutor;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.lep.api.LepResourceService;
import com.icthh.xm.lep.groovy.DefaultScriptNameLepResourceKeyMapper;
import com.icthh.xm.lep.groovy.ScriptNameLepResourceKeyMapper;
import com.icthh.xm.lep.groovy.StrategyGroovyLepExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.ResourceLoader;

import java.util.HashMap;
import java.util.List;
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

    @Autowired @Lazy
    private LoggingConfigService loggingConfigService;

    @Autowired @Lazy
    private TenantAliasService tenantAliasService;

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
            lepResourceService(),
            loggingConfigService);
    }

    @Bean
    public ScriptNameLepResourceKeyMapper scriptNameLepResourceKeyMapper() {
        return new DefaultScriptNameLepResourceKeyMapper();
    }

    @Bean
    public XmGroovyScriptEngineProviderStrategy xmGroovyScriptEngineProviderStrategy() {
        return new XmGroovyScriptEngineProviderStrategy(scriptNameLepResourceKeyMapper(),
                                                        appName,
                                                        lepResourceService(),
                                                        tenantAliasService);
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
    public XmLepScriptConfigServerResourceLoader cfgResourceLoader(List<CacheableLepEngine> cacheableEngines,
                                                                   @Value("${application.lep.full-recompile-on-lep-update:false}") Boolean fullRecompileOnLepUpdate) {
        return new XmLepScriptConfigServerResourceLoader(appName, cacheableEngines, fullRecompileOnLepUpdate);
    }

    @Bean
    public RouterResourceLoader routerResourceLoader() {
        Map<String, ResourceLoader> routerMap = new HashMap<>(RESOURCE_LOADERS_CAPACITY);
        routerMap.put(CLASSPATH_URL_PREFIX, resourceLoader);
        routerMap.put(XM_MS_CONFIG_URL_PREFIX, xmLepScriptConfigServerResourceLoader);
        routerMap.put(FILE_URL_PREFIX, xmFileSystemResourceLoader());
        return new RouterResourceLoader(routerMap);
    }

    protected abstract TenantScriptStorage getTenantScriptStorageType();

    @Bean
    public XmFileSystemResourceLoader xmFileSystemResourceLoader() {
        return new XmFileSystemResourceLoader(new FileSystemResourceLoader(),
                                              tenantAliasService(),
                                              appName);
    }

    @Bean
    public LepResourceService lepResourceService() {
        return new XmLepResourceService(appName,
                                        resolveResolver(),
                                        routerResourceLoader());
    }

    @Bean
    public TenantAliasService tenantAliasService() {
        return new TenantAliasService();
    }

    private TenantScriptPathResolver resolveResolver() {
        Map<TenantScriptStorage, TenantScriptPathResolver> resolverMap = new HashMap<>();
        resolverMap.put(CLASSPATH, new ClassPathTenantScriptPathResolver());
        resolverMap.put(XM_MS_CONFIG, new XmMsConfigTenantScriptPathResolver());
        resolverMap.put(FILE, new FileTenantScriptPathResolver(getFileTenantScriptPathResolverBaseDir()));
        return resolverMap.get(getTenantScriptStorageType());
    }

    protected String getFileTenantScriptPathResolverBaseDir() {
        return FileSystemUtils.getAppHomeDir();
    }

}
