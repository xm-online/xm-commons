package com.icthh.xm.commons.lep.groovy;

import com.codahale.metrics.MetricRegistry;
import com.icthh.xm.commons.lep.LepPathResolver;
import com.icthh.xm.commons.lep.groovy.storage.LepStorage;
import com.icthh.xm.commons.lep.impl.LoggingWrapper;

import java.util.Map;

public class RecreateGroovyLepEngineOnRefresh implements GroovyEngineCreationStrategy {

    public GroovyLepEngine createEngine(String tenant,
                                        LepStorage leps,
                                        LoggingWrapper loggingWrapper,
                                        ClassLoader classLoader,
                                        Map<String, GroovyFileParser.GroovyFileMetadata> lepMetadata,
                                        LepResourceConnector lepResourceConnector,
                                        LepPathResolver lepPathResolver,
                                        MetricRegistry metricRegistry,
                                        boolean isWarmupEnabled) {
        return new GroovyLepEngine(
            tenant,
            leps,
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
