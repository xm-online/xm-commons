package com.icthh.xm.commons.config.client.repository.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.config.client.api.ConfigService;
import com.icthh.xm.commons.config.client.api.FetchConfigurationSettings;
import com.icthh.xm.commons.config.client.applied.AppliedConfigService;
import com.icthh.xm.commons.config.client.repository.CommonConfigRepository;
import com.icthh.xm.commons.config.client.service.CommonConfigService;
import com.icthh.xm.commons.config.domain.Configuration;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class ConfigTopicConsumerUnitTest {

    @InjectMocks
    private ConfigTopicConsumer configTopicConsumer;
    @Mock
    private ConfigService configService;

    @Test
    public void consumeEvent() {
        configTopicConsumer.consumeEvent(record("\"commit\"", "\"path\""));

        verify(configService).updateConfigurations("commit", Collections.singleton("path"));
    }

    private static ConsumerRecord<String, String> record(String commitJson, String pathsJson) {
        return new ConsumerRecord<>("config", 1, 1, "key", "{\"eventId\":\"id\",\"commit\":" + commitJson
            + ",\"paths\":[" + pathsJson + "]}");
    }

    /** A real config service over a mocked repository, with the applied tracker registered as the first listener. */
    private static class Fixture {
        final CommonConfigRepository repository = mock(CommonConfigRepository.class);
        final CommonConfigService service =
            new CommonConfigService(new FetchConfigurationSettings("entity", false), repository);
        final AppliedConfigService applied = new AppliedConfigService(repository, new FetchConfigurationSettings("entity", false), true, "SYSTEM-ADMIN");
        final ConfigTopicConsumer consumer = new ConfigTopicConsumer(service, applied);

        Fixture() {
            service.addConfigurationChangedListener(applied);
        }
    }

    @Test
    public void eventWithOnlyIrrelevantPathsUpdatesCommitWithoutFetching() {
        Fixture f = new Fixture();

        f.consumer.consumeEvent(record("\"B\"", "\"/config/tenants/XM/uaa/uaa.yml\""));

        assertThat(f.applied.getVersion().mainVersion()).isEqualTo("B");
        assertThat(f.applied.getVersion().source()).isEqualTo("event");
        verify(f.repository, never()).getConfig(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    public void commitIsRecordedAfterListenersRan() {
        Fixture f = new Fixture();
        String path = "/config/tenants/XM/entity/a.yml";
        when(f.repository.getConfig("B", List.of(path))).thenReturn(Map.of(path, new Configuration(path, "a")));
        List<String> versionSeenByListener = new ArrayList<>();
        f.service.addConfigurationChangedListener(
            c -> versionSeenByListener.add(f.applied.getVersion().mainVersion()));

        f.consumer.consumeEvent(record("\"B\"", "\"" + path + "\""));

        assertThat(versionSeenByListener).containsExactly("unknown");
        assertThat(f.applied.getVersion().mainVersion()).isEqualTo("B");
        assertThat(f.applied.getPathHashes()).hasSize(1);
    }

    @Test
    public void failedEventLeavesVersionUnchanged() {
        Fixture f = new Fixture();
        String path = "/config/tenants/XM/entity/a.yml";
        when(f.repository.getConfig("B", List.of(path))).thenReturn(Map.of(path, new Configuration(path, "a")));
        f.service.addConfigurationChangedListener(c -> {
            throw new IllegalStateException("listener failed");
        });

        assertThrows(IllegalStateException.class, () -> f.consumer.consumeEvent(record("\"B\"", "\"" + path + "\"")));

        assertThat(f.applied.getVersion().mainVersion()).isEqualTo("unknown");
    }

    @Test
    public void jsonCommitIsParsed() {
        Fixture f = new Fixture();

        f.consumer.consumeEvent(record("\"{\\\"mainVersion\\\":\\\"m1\\\",\\\"externalTenantVersions\\\":"
            + "{\\\"T\\\":{\\\"mainVersion\\\":\\\"t1\\\"}}}\"", "\"/x\""));

        assertThat(f.applied.getVersion().mainVersion()).isEqualTo("m1");
        assertThat(f.applied.getVersion().externalTenantVersions()).containsEntry("T", "t1");
    }

    @Test
    public void malformedCommitDoesNotBreakEventProcessing() {
        Fixture f = new Fixture();
        String path = "/config/tenants/XM/entity/a.yml";
        when(f.repository.getConfig("{broken", List.of(path))).thenReturn(Map.of(path, new Configuration(path, "a")));

        f.consumer.consumeEvent(record("\"{broken\"", "\"" + path + "\""));

        assertThat(f.applied.getPathHashes()).hasSize(1);
        assertThat(f.applied.getVersion().mainVersion()).isNull();
        assertThat(f.applied.getVersion().raw()).isEqualTo("{broken");
    }
}
