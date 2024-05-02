package com.icthh.xm.commons.lep.groovy;

import com.icthh.xm.commons.config.client.service.TenantAliasService;
import com.icthh.xm.commons.lep.FileSystemUtils;
import com.icthh.xm.commons.lep.LepPathResolver;
import com.icthh.xm.commons.lep.TenantScriptStorage;
import com.icthh.xm.commons.lep.groovy.storage.ClassPathLepStorageFactory;
import com.icthh.xm.commons.lep.groovy.storage.FileLepStorageFactory;
import com.icthh.xm.commons.lep.groovy.storage.LepStorageFactory;
import com.icthh.xm.commons.lep.groovy.storage.XmConfigLepStorageFactory;
import com.icthh.xm.commons.lep.impl.LoggingWrapper;
import com.icthh.xm.commons.lep.impl.utils.ClassPathLepRepository;
import com.icthh.xm.commons.lep.spring.ApplicationNameProvider;
import com.icthh.xm.commons.lep.spring.LepSpringConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
    public GroovyMapLepWrapperFactory groovyMapLepWrapperFactory() {
        return new GroovyMapLepWrapperFactory();
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
