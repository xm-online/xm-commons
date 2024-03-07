package com.icthh.xm.commons.lep.groovy;

import com.icthh.xm.commons.config.client.service.TenantAliasService;
import com.icthh.xm.commons.lep.FileSystemUtils;
import com.icthh.xm.commons.lep.LepPathResolver;
import com.icthh.xm.commons.lep.TenantScriptStorage;
import com.icthh.xm.commons.lep.api.LepAdditionalContext;
import com.icthh.xm.commons.lep.api.LepContextFactory;
import com.icthh.xm.commons.lep.commons.CommonsService;
import com.icthh.xm.commons.lep.groovy.storage.ClassPathLepStorageFactory;
import com.icthh.xm.commons.lep.groovy.storage.FileLepStorageFactory;
import com.icthh.xm.commons.lep.groovy.storage.LepStorageFactory;
import com.icthh.xm.commons.lep.groovy.storage.XmConfigLepStorageFactory;
import com.icthh.xm.commons.lep.impl.LoggingWrapper;
import com.icthh.xm.commons.lep.impl.utils.ClassPathLepRepository;
import com.icthh.xm.commons.lep.spring.ApplicationNameProvider;
import com.icthh.xm.commons.lep.spring.LepContextService;
import com.icthh.xm.commons.lep.spring.LepSpringConfiguration;
import com.icthh.xm.commons.lep.spring.LepThreadHelper;
import com.icthh.xm.commons.lep.spring.lepservice.LepServiceFactoryWithLepFactoryMethod;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.icthh.xm.commons.lep.TenantScriptStorage.CLASSPATH;
import static com.icthh.xm.commons.lep.TenantScriptStorage.FILE;
import static com.icthh.xm.commons.lep.TenantScriptStorage.XM_MS_CONFIG;
import static java.util.Collections.emptySet;

@Configuration
@ConditionalOnMissingBean(GroovyLepEngineConfiguration.class)
public class GroovyLepEngineConfiguration extends LepSpringConfiguration {

    @Value("${application.lep.tenant-script-storage:#{T(com.icthh.xm.commons.lep.TenantScriptStorage).XM_MS_CONFIG}}")
    private TenantScriptStorage tenantScriptStorageType;

    @Value("${application.lep.warmup-scripts:true}")
    private boolean warmupScripts;

    @Value("${application.lep.tenants-with-lep-warmup:#{T(java.util.Set).of('XM')}}")
    private Set<String> tenantsWithLepWarmup;

    public GroovyLepEngineConfiguration(@Value("${spring.application.name}") String appName) {
        super(appName);
    }

    @Bean
    public GroovyLepEngineFactory groovyLepEngineFactory(ApplicationNameProvider applicationNameProvider,
                                                         LepStorageFactory lepStorageFactory,
                                                         LoggingWrapper loggingWrapper,
                                                         LepPathResolver lepPathResolver,
                                                         GroovyFileParser groovyFileParser) {
        String appName = applicationNameProvider.getAppName();
        return new GroovyLepEngineFactory(
            appName,
            lepStorageFactory,
            loggingWrapper,
            lepPathResolver,
            groovyFileParser,
            warmupScripts ? tenantsWithLepWarmup : emptySet()
        );
    }

    @Bean
    @Override
    public LepContextService lepContextService(LepContextFactory lepContextFactory,
                                               LepServiceFactoryWithLepFactoryMethod lepServiceFactory,
                                               LepThreadHelper lepThreadHelper,
                                               TenantContextHolder tenantContextHolder,
                                               XmAuthenticationContextHolder xmAuthContextHolder,
                                               // spring required Optional to allow empty lists
                                               Optional<List<LepAdditionalContext<?>>> additionalContexts,
                                               CommonsService commonsService) {
        LepContextService lepContextService = super.lepContextService(
            lepContextFactory,
            lepServiceFactory,
            lepThreadHelper,
            tenantContextHolder,
            xmAuthContextHolder,
            additionalContexts,
            commonsService
        );
        return new GroovyMapLepWrapperFactory(lepContextService);
    }

    @Bean
    @ConditionalOnMissingBean(GroovyFileParser.class)
    public GroovyFileParser groovyFileParser() {
        return new GroovyFileParser();
    }

    @Bean
    public LepStorageFactory lepStorageFactory(ApplicationNameProvider applicationNameProvider,
                                               ClassPathLepRepository classPathLepRepository,
                                               LepPathResolver lepPathResolver,
                                               TenantAliasService tenantAliasService) {
        String appName = applicationNameProvider.getAppName();
        TenantScriptStorage storageType = getTenantScriptStorageType();
        if (storageType.equals(XM_MS_CONFIG)) {
            return new XmConfigLepStorageFactory(appName, classPathLepRepository);
        } else if (storageType.equals(CLASSPATH)) {
            return new ClassPathLepStorageFactory(appName, classPathLepRepository, tenantAliasService);
        } else if (storageType.equals(FILE)) {
            return new FileLepStorageFactory(appName, classPathLepRepository, tenantAliasService, lepPathResolver, getFileTenantScriptPathResolverBaseDir());
        } else {
            throw new RuntimeException("Unsupported storage type");
        }

    }

    protected String getFileTenantScriptPathResolverBaseDir() {
        return FileSystemUtils.getAppHomeDir();
    }

    protected TenantScriptStorage getTenantScriptStorageType() {
        return tenantScriptStorageType;
    }

}
