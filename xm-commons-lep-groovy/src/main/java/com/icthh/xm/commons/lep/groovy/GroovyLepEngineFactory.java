package com.icthh.xm.commons.lep.groovy;

import com.icthh.xm.commons.config.client.service.TenantAliasService;
import com.icthh.xm.commons.lep.LepPathResolver;
import com.icthh.xm.commons.lep.api.LepEngine;
import com.icthh.xm.commons.lep.api.LepEngineFactory;
import com.icthh.xm.commons.lep.api.XmLepConfigFile;
import com.icthh.xm.commons.lep.groovy.storage.LepStorage;
import com.icthh.xm.commons.lep.groovy.storage.LepStorageFactory;
import groovy.util.logging.Slf4j;
import org.springframework.beans.factory.BeanClassLoaderAware;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class GroovyLepEngineFactory extends LepEngineFactory implements BeanClassLoaderAware {

    private final LepStorageFactory lepStorageFactory;
    private final LoggingWrapper loggingWrapper;
    private final GroovyFileParser groovyFileParser;
    private final LepPathResolver lepPathResolver;
    private final boolean isWarmupEnabled;

    private volatile ClassLoader classLoader;

    public GroovyLepEngineFactory(String appName,
                                  LepStorageFactory lepStorageFactory,
                                  LoggingWrapper loggingWrapper,
                                  LepPathResolver lepPathResolver,
                                  GroovyFileParser groovyFileParser,
                                  boolean isWarmupEnabled) {
        super(appName);
        this.lepPathResolver = lepPathResolver;
        this.lepStorageFactory = lepStorageFactory;
        this.loggingWrapper = loggingWrapper;
        this.groovyFileParser = groovyFileParser;
        this.isWarmupEnabled = isWarmupEnabled;
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public LepEngine createLepEngine(String tenant, List<XmLepConfigFile> lepFromConfig) {
        LepStorage lepConfigStorage = lepStorageFactory.buildXmConfigLepStorage(tenant, lepFromConfig);

        Map<String, GroovyFileParser.GroovyFileMetadata> lepMetadata = new HashMap<>();
        lepConfigStorage.forEach(lep -> lepMetadata.put(lep.metadataKey(), groovyFileParser.getFileMetaData(lep.readContent())));

        LepResourceConnector lepResourceConnector = new LepResourceConnector(
            tenant,
            lepPathResolver,
            lepConfigStorage,
            lepMetadata,
            groovyFileParser
        );
        return new GroovyLepEngine(
            tenant,
            lepConfigStorage,
            loggingWrapper,
            classLoader,
            lepMetadata,
            lepResourceConnector,
            lepPathResolver,
            isWarmupEnabled
        );
    }
}
