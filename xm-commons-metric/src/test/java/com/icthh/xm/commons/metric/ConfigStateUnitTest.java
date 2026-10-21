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

    private static final String BIG_PATH = "/config/tenants/XM/test/big.yml";
    private static final String SMALL_PATH = "/config/tenants/XM/test/small.yml";

    @Mock
    private CommonConfigRepository commonConfigRepository;

    @Test
    public void instancesWithSameConfigsHaveSameHashRegardlessOfUpdateHistory() {
        String hugeContent = "key: value\n".repeat(5_000_000);
        Configuration big = new Configuration(BIG_PATH, hugeContent);
        Configuration smallV1 = new Configuration(SMALL_PATH, "version: 1");
        Configuration smallV2 = new Configuration(SMALL_PATH, "version: 2");

        ConfigStateHolder restartedInstance = new ConfigStateHolder();
        CommonConfigService configService = new CommonConfigService(
            new FetchConfigurationSettings("test", true), commonConfigRepository, restartedInstance);

        MeterRegistry registry = new SimpleMeterRegistry();
        ConfigStateMetrics metrics = new ConfigStateMetrics(restartedInstance);
        metrics.bindTo(registry);

        assertThat(gauge(registry, RECEIVED_COMMIT)).isZero();
        assertThat(gauge(registry, PROCESSED_COMMIT)).isZero();
        assertThat(gauge(registry, PROCESSED_CONFIGS_HASH)).isZero();

        Map<String, Configuration> initialState = Map.of(BIG_PATH, big, SMALL_PATH, smallV1);
        restartedInstance.onConfigurationsProcessed(initialState);
        String initialHash = restartedInstance.getProcessedConfigsHash();

        Map<String, Configuration> update = Map.of(SMALL_PATH, smallV2);
        when(commonConfigRepository.getConfig(eq("commit2"), anyList())).thenReturn(update);
        configService.updateConfigurations("commit2", List.of(SMALL_PATH));

        assertThat(restartedInstance.getReceivedCommit()).isEqualTo("commit2");
        assertThat(restartedInstance.getProcessedCommit()).isEqualTo("commit2");
        assertThat(restartedInstance.getProcessedConfigsHash()).isNotEqualTo(initialHash);

        ConfigStateHolder freshInstance = new ConfigStateHolder();
        Map<String, Configuration> sameState = Map.of(BIG_PATH, big, SMALL_PATH, smallV2);
        freshInstance.onConfigurationsProcessed(sameState);

        assertThat(restartedInstance.getProcessedConfigsHash())
            .isEqualTo(freshInstance.getProcessedConfigsHash());

        assertThat(gauge(registry, RECEIVED_COMMIT)).isEqualTo("commit2".hashCode());
        assertThat(gauge(registry, PROCESSED_COMMIT)).isEqualTo("commit2".hashCode());
        assertThat(gauge(registry, PROCESSED_CONFIGS_HASH))
            .isEqualTo(restartedInstance.getProcessedConfigsHash().hashCode());

        Info.Builder builder = new Info.Builder();
        metrics.contribute(builder);
        assertThat(builder.build().getDetails()).containsEntry(DETAIL_NAME, Map.of(
            RECEIVED_COMMIT, "commit2",
            PROCESSED_COMMIT, "commit2",
            PROCESSED_CONFIGS_HASH, restartedInstance.getProcessedConfigsHash()
        ));

        Map<String, Configuration> deletion = Map.of(SMALL_PATH, new Configuration(SMALL_PATH, null));
        when(commonConfigRepository.getConfig(eq("commit3"), anyList())).thenReturn(deletion);
        configService.updateConfigurations("commit3", List.of(SMALL_PATH));

        ConfigStateHolder withoutSmall = new ConfigStateHolder();
        Map<String, Configuration> bigOnly = Map.of(BIG_PATH, big);
        withoutSmall.onConfigurationsProcessed(bigOnly);

        assertThat(restartedInstance.getProcessedConfigsHash())
            .isEqualTo(withoutSmall.getProcessedConfigsHash());
    }

    private static double gauge(MeterRegistry registry, String name) {
        return registry.get(name).gauge().value();
    }
}
