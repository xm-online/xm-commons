package com.icthh.xm.commons.lep.groovy;

import com.icthh.xm.commons.config.client.service.TenantAliasService;
import com.icthh.xm.commons.lep.FileSystemUtils;
import com.icthh.xm.commons.lep.TenantScriptStorage;
import com.icthh.xm.commons.lep.groovy.storage.ClassPathLepStorageFactory;
import com.icthh.xm.commons.lep.groovy.storage.FileLepStorageFactory;
import com.icthh.xm.commons.lep.groovy.storage.LepStorageFactory;
import com.icthh.xm.commons.lep.groovy.storage.XmConfigLepStorageFactory;
import com.icthh.xm.commons.lep.impl.utils.ClassPathLepRepository;
import com.icthh.xm.commons.lep.spring.ApplicationNameProvider;
import com.icthh.xm.commons.lep.spring.LepSpringConfiguration;
import com.icthh.xm.commons.logging.config.LoggingConfigService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.icthh.xm.commons.lep.TenantScriptStorage.CLASSPATH;
import static com.icthh.xm.commons.lep.TenantScriptStorage.FILE;
import static com.icthh.xm.commons.lep.TenantScriptStorage.FILE_FULL_UPDATE;
import static com.icthh.xm.commons.lep.TenantScriptStorage.XM_MS_CONFIG;

@Configuration
@ConditionalOnMissingBean(GroovyLepEngineConfiguration.class)
public class GroovyLepEngineConfiguration extends LepSpringConfiguration {

    @Value("${application.lep.tenant-script-storage:#{T(com.icthh.xm.commons.lep.TenantScriptStorage).XM_MS_CONFIG}}")
    private TenantScriptStorage tenantScriptStorageType;

    @Value("${application.lep.warmup-scripts:true}")
    private boolean warmupScripts;

    public GroovyLepEngineConfiguration(@Value("${spring.application.name}") String appName) {
        super(appName);
    }

    @Bean
    public GroovyLepEngineFactory groovyLepEngineFactory(ApplicationNameProvider applicationNameProvider,
                                                         TenantAliasService tenantAliasService,
                                                         LepStorageFactory lepStorageFactory,
                                                         LoggingWrapper loggingWrapper,
                                                         GroovyFileParser groovyFileParser) {
        String appName = applicationNameProvider.getAppName();
        return new GroovyLepEngineFactory(appName, tenantAliasService, lepStorageFactory, loggingWrapper, groovyFileParser, warmupScripts);
    }

    @Bean
    @ConditionalOnMissingBean(GroovyFileParser.class)
    public GroovyFileParser groovyFileParser() {
        return new GroovyFileParser();
    }

    @Bean
    @ConditionalOnMissingBean(LoggingWrapper.class)
    public LoggingWrapper loggingWrapper(LoggingConfigService loggingConfigService) {
        return new LoggingWrapper(loggingConfigService);
    }

    @Bean
    public LepStorageFactory lepStorageFactory(ApplicationNameProvider applicationNameProvider,
                                               ClassPathLepRepository classPathLepRepository,
                                               TenantAliasService tenantAliasService) {
        String appName = applicationNameProvider.getAppName();
        TenantScriptStorage storageType = getTenantScriptStorageType();
        if (storageType.equals(XM_MS_CONFIG)) {
            return new XmConfigLepStorageFactory(appName, classPathLepRepository);
        } else if (storageType.equals(CLASSPATH)) {
            return new ClassPathLepStorageFactory(appName, classPathLepRepository, tenantAliasService);
        } else if (storageType.equals(FILE)) {
            return new FileLepStorageFactory(appName, classPathLepRepository, tenantAliasService, getFileTenantScriptPathResolverBaseDir());
        } else if (storageType.equals(FILE_FULL_UPDATE)) {
            return null;
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
