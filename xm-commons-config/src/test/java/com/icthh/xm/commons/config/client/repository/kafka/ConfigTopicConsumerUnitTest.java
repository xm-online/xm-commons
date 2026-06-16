package com.icthh.xm.commons.config.client.repository.kafka;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.config.client.api.ConfigService;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

@RunWith(MockitoJUnitRunner.class)
public class ConfigTopicConsumerUnitTest {

    @InjectMocks
    private ConfigTopicConsumer configTopicConsumer;
    @Mock
    private ConfigService configService;
    @Mock
    private List<RefreshableConfiguration> refreshableConfigurations;
    @Mock
    private RefreshableConfiguration refreshableConfiguration;

    @Test
    public void consumeEvent() {
        when(refreshableConfigurations.stream()).thenAnswer(invocation -> Stream.of(refreshableConfiguration));
        when(refreshableConfiguration.isListeningConfiguration(eq("path"))).thenReturn(true);
        when(refreshableConfiguration.isListeningConfiguration(eq("ignoredPath"))).thenReturn(false);

        configTopicConsumer.consumeEvent(new ConsumerRecord<>("config", 1, 1, "key", "{\n"
            + "    \"eventId\":\"id\",\n"
            + "    \"commit\":\"commit\",\n"
            + "    \"paths\":[\"path\", \"ignoredPath\"]\n"
            + "}"));

        verify(configService).updateConfigurations("commit", Collections.singleton("path"));
    }
}
