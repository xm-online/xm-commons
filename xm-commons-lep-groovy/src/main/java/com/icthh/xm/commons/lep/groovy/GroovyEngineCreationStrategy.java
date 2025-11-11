package com.icthh.xm.commons.lep.groovy;

import com.codahale.metrics.MetricRegistry;
import com.icthh.xm.commons.lep.LepPathResolver;
import com.icthh.xm.commons.lep.groovy.storage.LepStorage;
import com.icthh.xm.commons.lep.impl.LoggingWrapper;

import java.util.Map;

public interface GroovyEngineCreationStrategy {

        GroovyLepEngine createEngine(String tenant,
                                     LepStorage leps,
                                     LoggingWrapper loggingWrapper,
                                     ClassLoader classLoader,
                                     Map<String, GroovyFileParser.GroovyFileMetadata> lepMetadata,
                                     LepResourceConnector lepResourceConnector,
                                     LepPathResolver lepPathResolver,
                                     MetricRegistry metricRegistry,
                                     boolean isWarmupEnabled);
}
