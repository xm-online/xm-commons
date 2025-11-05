package com.icthh.xm.commons.lep.groovy;

import com.icthh.xm.commons.lep.LepPathResolver;
import com.icthh.xm.commons.lep.groovy.storage.LepStorage;
import com.icthh.xm.commons.lep.impl.LoggingWrapper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CachedGroovyLepEngineCreationStrategy implements GroovyEngineCreationStrategy {

    private final Map<String, GroovyLepEngine> engineByTenant = new ConcurrentHashMap<>();

    public GroovyLepEngine createEngine(String tenant,
                                        LepStorage leps,
                                        LoggingWrapper loggingWrapper,
                                        ClassLoader classLoader,
                                        Map<String, GroovyFileParser.GroovyFileMetadata> lepMetadata,
                                        LepResourceConnector lepResourceConnector,
                                        LepPathResolver lepPathResolver,
                                        boolean isWarmupEnabled,
                                        boolean loadPrecompiled,
                                        String precompiledInputDirectory,
                                        String compilationOutputDirectory) {
        return engineByTenant.computeIfAbsent(tenant, it ->
            new GroovyLepEngine(
                tenant,
                leps,
                loggingWrapper,
                classLoader,
                lepMetadata,
                lepResourceConnector,
                lepPathResolver,
                isWarmupEnabled,
                loadPrecompiled,
                precompiledInputDirectory,
                compilationOutputDirectory)
        );
    }
}
