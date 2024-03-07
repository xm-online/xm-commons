package com.icthh.xm.commons.lep.spring;

import com.icthh.xm.commons.config.client.service.TenantAliasService;
import com.icthh.xm.commons.lep.DefaultLepKeyResolver;
import com.icthh.xm.commons.lep.LepPathResolver;
import com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader;
import com.icthh.xm.commons.lep.api.BaseLepContext;
import com.icthh.xm.commons.lep.api.LepAdditionalContext;
import com.icthh.xm.commons.lep.api.LepContextFactory;
import com.icthh.xm.commons.lep.api.LepEngine;
import com.icthh.xm.commons.lep.api.LepEngineFactory;
import com.icthh.xm.commons.lep.api.LepManagementService;
import com.icthh.xm.commons.lep.commons.CommonsConfiguration;
import com.icthh.xm.commons.lep.commons.CommonsService;
import com.icthh.xm.commons.lep.impl.LepMethodAspect;
import com.icthh.xm.commons.lep.impl.LoggingWrapper;
import com.icthh.xm.commons.lep.impl.LogicExtensionPointHandler;
import com.icthh.xm.commons.lep.impl.engine.LepManagementServiceImpl;
import com.icthh.xm.commons.lep.impl.internal.MigrationFromCoreContextsHolderLepManagementServiceReference;
import com.icthh.xm.commons.lep.impl.utils.ClassPathLepRepository;
import com.icthh.xm.commons.lep.spring.lepservice.ClearServicesOnEngineDestroy;
import com.icthh.xm.commons.lep.spring.lepservice.LepServiceFactoryResolver;
import com.icthh.xm.commons.lep.spring.lepservice.LepServiceFactoryWithLepFactoryMethod;
import com.icthh.xm.commons.logging.config.LoggingConfigService;
import com.icthh.xm.commons.logging.util.BasePackageDetector;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.lep.api.LepKeyResolver;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.lep.core.CoreLepManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;

import java.util.List;

@Configuration
@ConditionalOnMissingBean(LepSpringConfiguration.class)
@EnableAspectJAutoProxy
@Import({CommonsConfiguration.class})
public class LepSpringConfiguration {

    private final String appName;

    public LepSpringConfiguration(@Value("${spring.application.name}") String appName) {
        this.appName = appName;
    }

    @Bean
    @ConditionalOnMissingBean(LepManagementService.class)
    public LepManagementService lepManagementService(List<LepEngineFactory> engineFactories,
                                                     TenantContextHolder tenantContextHolder,
                                                     List<LepEngine.DestroyCallback> destroyCallbacks) {
        return new LepManagementServiceImpl(engineFactories, tenantContextHolder, destroyCallbacks);
    }

    @Bean
    @ConditionalOnMissingBean(ApplicationNameProvider.class)
    public ApplicationNameProvider applicationNameProvider() {
        return new ApplicationNameProvider(appName);
    }

    @Bean
    @ConditionalOnMissingBean(LepUpdateMode.class)
    public LepUpdateMode lepUpdateMode() {
        return LepUpdateMode.LIVE;
    }

    @Bean
    @ConditionalOnMissingBean(XmLepScriptConfigServerResourceLoader.class)
    public XmLepScriptConfigServerResourceLoader cfgResourceLoader(LepPathResolver lepPathResolver,
                                                                   LepManagementService lepManagementService,
                                                                   LepUpdateMode lepUpdateMode,
                                                                   TenantContextHolder tenantContextHolder) {
        return new XmLepScriptConfigServerResourceLoader(
            lepPathResolver,
            lepManagementService,
            lepUpdateMode,
            tenantContextHolder
        );
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
    public TenantAliasService tenantAliasService() {
        return new TenantAliasService();
    }

    @Bean
    public LepMethodAspect lepMethodAspect() {
        return new LepMethodAspect();
    }

    @Bean
    public LogicExtensionPointHandler logicExtensionPointHandler(List<LepKeyResolver> resolverList,
                                                                 LepManagementService lepEngineService,
                                                                 LepContextService lepContextService) {
        return new LogicExtensionPointHandler(resolverList, lepEngineService, lepContextService);
    }

    @Bean
    @ConditionalOnMissingBean(LepContextFactory.class)
    public LepContextFactory lepContextFactory() {
        return lepMethod -> new BaseLepContext() {};
    }

    @Bean
    public DefaultLepKeyResolver defaultLepKeyResolver() {
        return new DefaultLepKeyResolver();
    }

    @Bean
    public LepServiceFactoryResolver lepServiceFactoryResolver() {
        return new LepServiceFactoryResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    public BasePackageDetector basePackageDetector(ApplicationContext applicationContext) {
        return new BasePackageDetector(applicationContext);
    }

    @Bean
    @ConditionalOnMissingBean
    public LepContextService lepContextService(LepContextFactory lepContextFactory,
                                               LepServiceFactoryWithLepFactoryMethod lepServiceFactory,
                                               LepThreadHelper lepThreadHelper,
                                               TenantContextHolder tenantContextHolder,
                                               XmAuthenticationContextHolder xmAuthContextHolder,
                                               List<LepAdditionalContext<?>> additionalContexts,
                                               CommonsService commonsService) {
        return new LepContextServiceImpl(
            lepContextFactory,
            lepServiceFactory,
            lepThreadHelper,
            tenantContextHolder,
            xmAuthContextHolder,
            additionalContexts,
            commonsService
        );
    }

    @Bean
    @ConditionalOnMissingBean(LepServiceFactoryWithLepFactoryMethod.class)
    public LepServiceFactoryWithLepFactoryMethod lepServiceFactory(
        @Value("${application.lep.service-factory-timeout:60}")
        Integer timeout
    ) {
        return new LepServiceFactoryWithLepFactoryMethod(timeout);
    }

    @Bean
    public ClearServicesOnEngineDestroy clearServicesOnEngineDestroy(LepServiceFactoryWithLepFactoryMethod factory) {
        return new ClearServicesOnEngineDestroy(factory);
    }

    @Bean
    @ConditionalOnMissingBean(LoggingWrapper.class)
    public LoggingWrapper loggingWrapper(LoggingConfigService loggingConfigService) {
        return new LoggingWrapper(loggingConfigService);
    }

    @Bean
    public LepThreadHelper lepThreadHelper(TenantContextHolder tenantContextHolder, LepManagementService lepManagementService) {
        return new LepThreadHelper(tenantContextHolder, lepManagementService);
    }

    @Bean
    @Deprecated(forRemoval = true)
    public LepManager lepManager(TenantContextHolder tenantContextHolder, LepManagementService lepManagementService) {
        return new CoreLepManager(tenantContextHolder, lepManagementService);
    }

    @Bean
    @Deprecated(forRemoval = true)
    public MigrationFromCoreContextsHolderLepManagementServiceReference migrationFromCoreContextsHolderLepManagementServiceReference(LepManagementService lepManagementService) {
        return new MigrationFromCoreContextsHolderLepManagementServiceReference(lepManagementService);
    }

}
