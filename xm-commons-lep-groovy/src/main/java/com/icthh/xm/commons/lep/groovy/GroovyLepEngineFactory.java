package com.icthh.xm.commons.lep.groovy;

import com.icthh.xm.commons.lep.LepPathResolver;
import com.icthh.xm.commons.lep.api.BaseLepContext;
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
import java.util.UUID;
import java.util.function.Supplier;

import static java.lang.Boolean.TRUE;

@Slf4j
public class GroovyLepEngineFactory extends LepEngineFactory implements BeanClassLoaderAware {

    private final LepStorageFactory lepStorageFactory;
    private final LoggingWrapper loggingWrapper;
    private final GroovyFileParser groovyFileParser;
    private final LepPathResolver lepPathResolver;
    private final Set<String> tenantWithWarmup;
    private final Boolean warmupScriptsForAllTenants;
    private final Boolean precompiledMode;
    private final String pathToWorkingDirectory;
    private final GroovyEngineCreationStrategy groovyEngineCreationStrategy;
    private final boolean clearMapConstructorTypeAnnotations;

    private volatile ClassLoader classLoader;
    private volatile Supplier<Class<? extends BaseLepContext>> sharedRuntimeWarmupLepContextClass;
    private volatile boolean sharedRuntimeWarmupEnabled;

    public GroovyLepEngineFactory(String appName,
                                  LepStorageFactory lepStorageFactory,
                                  GroovyEngineCreationStrategy groovyEngineCreationStrategy,
                                  LoggingWrapper loggingWrapper,
                                  LepPathResolver lepPathResolver,
                                  GroovyFileParser groovyFileParser,
                                  Set<String> tenantWithWarmup,
                                  Boolean warmupScriptsForAllTenants,
                                  Boolean precompiledMode,
                                  String pathToWorkingDirectory) {
        this(appName, lepStorageFactory, groovyEngineCreationStrategy, loggingWrapper, lepPathResolver,
            groovyFileParser, tenantWithWarmup, warmupScriptsForAllTenants, precompiledMode,
            pathToWorkingDirectory, true);
    }

    public GroovyLepEngineFactory(String appName,
                                  LepStorageFactory lepStorageFactory,
                                  GroovyEngineCreationStrategy groovyEngineCreationStrategy,
                                  LoggingWrapper loggingWrapper,
                                  LepPathResolver lepPathResolver,
                                  GroovyFileParser groovyFileParser,
                                  Set<String> tenantWithWarmup,
                                  Boolean warmupScriptsForAllTenants,
                                  Boolean precompiledMode,
                                  String pathToWorkingDirectory,
                                  boolean clearMapConstructorTypeAnnotations) {
        super(appName);
        this.clearMapConstructorTypeAnnotations = clearMapConstructorTypeAnnotations;
        this.lepPathResolver = lepPathResolver;
        this.lepStorageFactory = lepStorageFactory;
        this.groovyEngineCreationStrategy = groovyEngineCreationStrategy;
        this.loggingWrapper = loggingWrapper;
        this.groovyFileParser = groovyFileParser;
        this.tenantWithWarmup = tenantWithWarmup;
        this.warmupScriptsForAllTenants = warmupScriptsForAllTenants;
        this.precompiledMode = precompiledMode;
        this.pathToWorkingDirectory = pathToWorkingDirectory;
    }

    /**
     * Turns on the once-per-JVM {@link GroovySharedRuntimeWarmup}. The trigger deliberately lives on this
     * factory and not on a standalone bean: the factory is a dependency of the whole lep subsystem, so
     * spring creates it - and the warmup starts - before any engine can be built, which is what lets the
     * engine warmup await the shared warmup before the "lep engines inited" latch opens. A standalone bean
     * has no dependents and is created at an arbitrary point of a minutes-long startup, losing that
     * ordering guarantee.
     */
    public void enableSharedRuntimeWarmup(Supplier<Class<? extends BaseLepContext>> lepContextClass) {
        this.sharedRuntimeWarmupLepContextClass = lepContextClass;
        this.sharedRuntimeWarmupEnabled = true;
        startSharedRuntimeWarmup();
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
        startSharedRuntimeWarmup();
    }

    private void startSharedRuntimeWarmup() {
        if (sharedRuntimeWarmupEnabled && classLoader != null) {
            GroovySharedRuntimeWarmup.warmupOnceAsync(classLoader, sharedRuntimeWarmupLepContextClass);
        }
    }

    @Override
    public LepEngine createLepEngine(String tenant, List<XmLepConfigFile> lepFromConfig) {
        return createLepEngine(tenant, lepFromConfig, pathToWorkingDirectory);
    }

    @Override
    public LepEngine createLepEngine(String tenant, List<XmLepConfigFile> lepFromConfig, String targetDirectoryPath) {
        LepStorage lepConfigStorage = lepStorageFactory.buildXmConfigLepStorage(tenant, lepFromConfig);

        String engineId = UUID.randomUUID().toString();
        Map<String, GroovyFileParser.GroovyFileMetadata> lepMetadata = new HashMap<>();
        lepConfigStorage.forEach(lep -> lepMetadata.put(
            lep.metadataKey(),
            groovyFileParser.getFileMetaData(engineId, lep.getPath(), lep.readContent())
        ));

        LepResourceConnector lepResourceConnector = new LepResourceConnector(
            engineId,
            tenant,
            lepPathResolver,
            lepConfigStorage,
            lepMetadata,
            groovyFileParser
        );

        boolean isWarmupEnabled = TRUE.equals(warmupScriptsForAllTenants) || tenantWithWarmup.contains(tenant);
        GroovyLepEngine engine;
        try {
            engine = groovyEngineCreationStrategy.createEngine(
                engineId,
                tenant,
                lepConfigStorage,
                loggingWrapper,
                classLoader,
                lepMetadata,
                lepResourceConnector,
                lepPathResolver,
                isWarmupEnabled,
                precompiledMode,
                targetDirectoryPath
            );
        } finally {
            // every generation compiled here leaves its @MapConstructor classes behind on a static
            // groovy node, and each one pins the compilation it came from; this is the boundary where
            // dropping them is safe
            if (clearMapConstructorTypeAnnotations) {
                GroovyMapConstructorTypeAnnotations.clear();
            }
        }

        // released by the local id and not by engine.getId(): a strategy that keeps one engine over
        // refreshes returns an engine of an earlier id, and every generation still has to be released
        engine.addDestroyCallback(destroyed -> groovyFileParser.releaseEngine(engineId));
        return engine;
    }
}
