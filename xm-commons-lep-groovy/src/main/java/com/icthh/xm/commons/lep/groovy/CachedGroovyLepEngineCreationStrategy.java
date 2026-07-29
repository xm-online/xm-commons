package com.icthh.xm.commons.lep.groovy;

import com.icthh.xm.commons.lep.LepPathResolver;
import com.icthh.xm.commons.lep.groovy.storage.LepStorage;
import com.icthh.xm.commons.lep.impl.LoggingWrapper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CachedGroovyLepEngineCreationStrategy implements GroovyEngineCreationStrategy {

    private final Map<String, GroovyLepEngine> engineByTenant = new ConcurrentHashMap<>();
    private final int minimumRecompilationInterval;

    public CachedGroovyLepEngineCreationStrategy(int minimumRecompilationInterval) {
        this.minimumRecompilationInterval = minimumRecompilationInterval;
    }

    public GroovyLepEngine createEngine(String engineId,
                                        String tenant,
                                        LepStorage leps,
                                        LoggingWrapper loggingWrapper,
                                        ClassLoader classLoader,
                                        Map<String, GroovyFileParser.GroovyFileMetadata> lepMetadata,
                                        LepResourceConnector lepResourceConnector,
                                        LepPathResolver lepPathResolver,
                                        boolean isWarmupEnabled,
                                        boolean useDirectoryCompiledSources,
                                        String targetDirectoryPath) {
        return engineByTenant.computeIfAbsent(tenant, it ->
            new GroovyLepEngine(
                engineId,
                tenant,
                leps,
                loggingWrapper,
                classLoader,
                lepMetadata,
                lepResourceConnector,
                lepPathResolver,
                isWarmupEnabled,
                useDirectoryCompiledSources,
                targetDirectoryPath,
                minimumRecompilationInterval
            )
        );
    }
}
