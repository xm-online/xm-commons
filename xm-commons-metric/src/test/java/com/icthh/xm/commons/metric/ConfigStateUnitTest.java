package com.icthh.xm.commons.metric;

import com.icthh.xm.commons.config.client.api.FetchConfigurationSettings;
import com.icthh.xm.commons.config.client.repository.CommonConfigRepository;
import com.icthh.xm.commons.config.client.service.CommonConfigService;
import com.icthh.xm.commons.config.client.state.ConfigStateHolder;
import com.icthh.xm.commons.config.domain.Configuration;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.info.Info;

import java.util.List;
import java.util.Map;

import static com.icthh.xm.commons.metric.ConfigStateMetrics.DETAIL_NAME;
import static com.icthh.xm.commons.metric.ConfigStateMetrics.PROCESSED_COMMIT;
import static com.icthh.xm.commons.metric.ConfigStateMetrics.PROCESSED_CONFIGS_HASH;
import static com.icthh.xm.commons.metric.ConfigStateMetrics.RECEIVED_COMMIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ConfigStateUnitTest {

    private static final String PATH = "/config/tenants/XM/test/big.yml";

    @Mock
    private CommonConfigRepository commonConfigRepository;

    @Test
    public void configStateIsExposedAsMetricsAndInfoAfterUpdate() {
        ConfigStateHolder configStateHolder = new ConfigStateHolder();
        CommonConfigService configService = new CommonConfigService(
            new FetchConfigurationSettings("test", true), commonConfigRepository, configStateHolder);

        MeterRegistry registry = new SimpleMeterRegistry();
        ConfigStateMetrics metrics = new ConfigStateMetrics(configStateHolder);
        metrics.bindTo(registry);

        assertThat(gauge(registry, RECEIVED_COMMIT)).isZero();
        assertThat(gauge(registry, PROCESSED_COMMIT)).isZero();
        assertThat(gauge(registry, PROCESSED_CONFIGS_HASH)).isZero();

        String hugeContent = "key: value\n".repeat(5_000_000);
        Map<String, Configuration> configurations = Map.of(PATH, new Configuration(PATH, hugeContent));
        when(commonConfigRepository.getConfig(eq("commit1"), anyList())).thenReturn(configurations);

        configService.updateConfigurations("commit1", List.of(PATH));

        String firstHash = configStateHolder.getProcessedConfigsHash();
        assertThat(configStateHolder.getReceivedCommit()).isEqualTo("commit1");
        assertThat(configStateHolder.getProcessedCommit()).isEqualTo("commit1");
        assertThat(firstHash).isNotNull();

        assertThat(gauge(registry, RECEIVED_COMMIT)).isEqualTo("commit1".hashCode());
        assertThat(gauge(registry, PROCESSED_COMMIT)).isEqualTo("commit1".hashCode());
        assertThat(gauge(registry, PROCESSED_CONFIGS_HASH)).isEqualTo(firstHash.hashCode());

        Info.Builder builder = new Info.Builder();
        metrics.contribute(builder);
        assertThat(builder.build().getDetails()).containsEntry(DETAIL_NAME, Map.of(
            RECEIVED_COMMIT, "commit1",
            PROCESSED_COMMIT, "commit1",
            PROCESSED_CONFIGS_HASH, firstHash
        ));

        Map<String, Configuration> changedConfigurations = Map.of(PATH, new Configuration(PATH, hugeContent + "x"));
        when(commonConfigRepository.getConfig(eq("commit2"), anyList())).thenReturn(changedConfigurations);

        configService.updateConfigurations("commit2", List.of(PATH));

        assertThat(configStateHolder.getProcessedCommit()).isEqualTo("commit2");
        assertThat(configStateHolder.getProcessedConfigsHash()).isNotEqualTo(firstHash);
    }

    private static double gauge(MeterRegistry registry, String name) {
        return registry.get(name).gauge().value();
    }
}
