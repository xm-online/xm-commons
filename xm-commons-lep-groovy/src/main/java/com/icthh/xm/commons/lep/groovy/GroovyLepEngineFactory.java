package com.icthh.xm.commons.lep.groovy;

import com.codahale.metrics.MetricRegistry;
import com.icthh.xm.commons.lep.LepPathResolver;
import com.icthh.xm.commons.lep.api.LepEngine;
import com.icthh.xm.commons.lep.api.LepEngineFactory;
import com.icthh.xm.commons.lep.api.XmLepConfigFile;
import com.icthh.xm.commons.lep.groovy.storage.LepStorage;
import com.icthh.xm.commons.lep.groovy.storage.LepStorageFactory;
import com.icthh.xm.commons.lep.impl.LoggingWrapper;
import groovy.util.logging.Slf4j;
import org.springframework.beans.factory.BeanClassLoaderAware;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.Boolean.TRUE;

@Slf4j
public class GroovyLepEngineFactory extends LepEngineFactory implements BeanClassLoaderAware {

    private final LepStorageFactory lepStorageFactory;
    private final LoggingWrapper loggingWrapper;
    private final GroovyFileParser groovyFileParser;
    private final LepPathResolver lepPathResolver;
    private final Set<String> tenantWithWarmup;
    private final Boolean warmupScriptsForAllTenants;
    private final GroovyEngineCreationStrategy groovyEngineCreationStrategy;
    private final MetricRegistry metricRegistry;

    private volatile ClassLoader classLoader;

    public GroovyLepEngineFactory(String appName,
                                  LepStorageFactory lepStorageFactory,
                                  GroovyEngineCreationStrategy groovyEngineCreationStrategy,
                                  LoggingWrapper loggingWrapper,
                                  LepPathResolver lepPathResolver,
                                  GroovyFileParser groovyFileParser,
                                  Set<String> tenantWithWarmup,
                                  Boolean warmupScriptsForAllTenants,
                                  MetricRegistry metricRegistry) {
        super(appName);
        this.lepPathResolver = lepPathResolver;
        this.lepStorageFactory = lepStorageFactory;
        this.groovyEngineCreationStrategy = groovyEngineCreationStrategy;
        this.loggingWrapper = loggingWrapper;
        this.groovyFileParser = groovyFileParser;
        this.tenantWithWarmup = tenantWithWarmup;
        this.warmupScriptsForAllTenants = warmupScriptsForAllTenants;
        this.metricRegistry = metricRegistry;
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

        boolean isWarmupEnabled = TRUE.equals(warmupScriptsForAllTenants) || tenantWithWarmup.contains(tenant);
        return groovyEngineCreationStrategy.createEngine(
            tenant,
            lepConfigStorage,
            loggingWrapper,
            classLoader,
            lepMetadata,
            lepResourceConnector,
            lepPathResolver,
            metricRegistry,
            isWarmupEnabled
        );
    }
}
