package com.icthh.xm.commons.lep.groovy;

import com.icthh.xm.commons.config.client.service.TenantAliasService;
import com.icthh.xm.commons.lep.api.LepEngine;
import com.icthh.xm.commons.lep.api.LepEngineFactory;
import com.icthh.xm.commons.lep.api.XmLepConfigFile;
import com.icthh.xm.commons.lep.groovy.storage.LepStorage;
import com.icthh.xm.commons.lep.groovy.storage.LepStorageFactory;
import groovy.util.logging.Slf4j;
import org.springframework.beans.factory.BeanClassLoaderAware;

import java.util.List;

@Slf4j
public class GroovyLepEngineFactory extends LepEngineFactory implements BeanClassLoaderAware {

    private final String appName;
    private final TenantAliasService tenantAliasService;
    private final LepStorageFactory lepStorageFactory;
    private final LoggingWrapper loggingWrapper;
    private final GroovyFileParser groovyFileParser;

    private volatile ClassLoader classLoader;

    public GroovyLepEngineFactory(String appName,
                                  TenantAliasService tenantAliasService,
                                  LepStorageFactory lepStorageFactory,
                                  LoggingWrapper loggingWrapper,
                                  GroovyFileParser groovyFileParser) {
        super(appName);
        this.appName = appName;
        this.tenantAliasService = tenantAliasService;
        this.lepStorageFactory = lepStorageFactory;
        this.loggingWrapper = loggingWrapper;
        this.groovyFileParser = groovyFileParser;
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public LepEngine createLepEngine(String tenant, List<XmLepConfigFile> lepFromConfig) {
        LepStorage lepConfigStorage = lepStorageFactory.buildXmConfigLepStorage(tenant, lepFromConfig);
        LepResourceConnector lepResourceConnector = new LepResourceConnector(
            tenant,
            appName,
            tenantAliasService,
            lepConfigStorage,
            groovyFileParser
        );
        return new GroovyLepEngine(
            appName,
            tenant,
            lepConfigStorage,
            loggingWrapper,
            classLoader,
            lepResourceConnector
        );
    }
}