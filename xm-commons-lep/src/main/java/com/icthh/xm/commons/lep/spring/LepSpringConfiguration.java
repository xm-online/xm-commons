package com.icthh.xm.commons.lep.spring;

import com.icthh.xm.commons.config.client.service.TenantAliasService;
import com.icthh.xm.commons.lep.DefaultLepKeyResolver;
import com.icthh.xm.commons.lep.RefreshTaskExecutor;
import com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader;
import com.icthh.xm.commons.lep.api.BaseLepContext;
import com.icthh.xm.commons.lep.api.LepContextFactory;
import com.icthh.xm.commons.lep.api.LepEngineFactory;
import com.icthh.xm.commons.lep.api.LepManagementService;
import com.icthh.xm.commons.lep.commons.CommonsConfiguration;
import com.icthh.xm.commons.lep.config.LepInterceptorConfiguration;
import com.icthh.xm.commons.lep.impl.LepMethodAspect;
import com.icthh.xm.commons.lep.impl.LogicExtensionPointHandler;
import com.icthh.xm.commons.lep.impl.engine.LepManagementServiceImpl;
import com.icthh.xm.commons.lep.impl.internal.MigrationFromCoreContextsHolderLepManagementServiceReference;
import com.icthh.xm.commons.lep.impl.utils.ClassPathLepRepository;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.lep.api.LepKeyResolver;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.lep.core.CoreLepManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;

import java.util.List;

@Configuration
@ConditionalOnMissingBean(LepSpringConfiguration.class)
@EnableAspectJAutoProxy
@Import({CommonsConfiguration.class, LepInterceptorConfiguration.class})
public class LepSpringConfiguration {

    private final String appName;

    public LepSpringConfiguration(@Value("${spring.application.name}") String appName) {
        this.appName = appName;
    }

    @Bean
    @ConditionalOnMissingBean(XmLepScriptConfigServerResourceLoader.class)
    public XmLepScriptConfigServerResourceLoader cfgResourceLoader(LepManagementService lepManagementService,
                                                                   RefreshTaskExecutor refreshTaskExecutor) {
        return new XmLepScriptConfigServerResourceLoader(appName, lepManagementService, refreshTaskExecutor);
    }

    @Bean
    @ConditionalOnMissingBean(RefreshTaskExecutor.class)
    public RefreshTaskExecutor refreshTaskExecutor() {
        return new RefreshTaskExecutor();
    }

    @Bean
    @ConditionalOnMissingBean(LepManagementService.class)
    public LepManagementService lepManagementService(List<LepEngineFactory> engineFactories, TenantContextHolder tenantContextHolder) {
        return new LepManagementServiceImpl(engineFactories, tenantContextHolder);
    }

    @Bean
    public ClassPathLepRepository classPathLepRepository() {
        return new ClassPathLepRepository();
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
    @Deprecated(forRemoval = true)
    public LepManager lepManager() {
        return new CoreLepManager();
    }

    @Bean
    @Deprecated(forRemoval = true)
    public MigrationFromCoreContextsHolderLepManagementServiceReference migrationFromCoreContextsHolderLepManagementServiceReference(LepManagementService lepManagementService) {
        return new MigrationFromCoreContextsHolderLepManagementServiceReference(lepManagementService);
    }

}
