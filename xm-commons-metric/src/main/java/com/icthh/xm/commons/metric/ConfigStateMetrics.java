package com.icthh.xm.commons.metric;

import com.icthh.xm.commons.config.client.state.ConfigStateHolder;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class ConfigStateMetrics implements MeterBinder, InfoContributor {

    public static final String DETAIL_NAME = "xmConfig";
    public static final String RECEIVED_COMMIT = "xm.config.received.commit";
    public static final String PROCESSED_COMMIT = "xm.config.processed.commit";
    public static final String PROCESSED_CONFIGS_HASH = "xm.config.processed.configs.hash";

    private final ConfigStateHolder configStateHolder;

    @Override
    public void bindTo(MeterRegistry registry) {
        register(registry, RECEIVED_COMMIT, configStateHolder::getReceivedCommit);
        register(registry, PROCESSED_COMMIT, configStateHolder::getProcessedCommit);
        register(registry, PROCESSED_CONFIGS_HASH, configStateHolder::getProcessedConfigsHash);
    }

    @Override
    public void contribute(Info.Builder builder) {
        Map<String, String> details = new LinkedHashMap<>();
        details.put(RECEIVED_COMMIT, configStateHolder.getReceivedCommit());
        details.put(PROCESSED_COMMIT, configStateHolder.getProcessedCommit());
        details.put(PROCESSED_CONFIGS_HASH, configStateHolder.getProcessedConfigsHash());
        builder.withDetail(DETAIL_NAME, details);
    }

    private static void register(MeterRegistry registry, String name, Supplier<String> value) {
        Gauge.builder(name, () -> toMeterValue(value.get())).register(registry);
    }

    private static int toMeterValue(String value) {
        return value == null ? 0 : value.hashCode();
    }
}
