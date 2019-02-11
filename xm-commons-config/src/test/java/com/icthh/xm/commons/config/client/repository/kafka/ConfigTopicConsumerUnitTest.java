package com.icthh.xm.commons.config.client.repository.kafka;

import static org.mockito.Mockito.verify;

import com.icthh.xm.commons.config.client.api.ConfigService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

@RunWith(MockitoJUnitRunner.class)
public class ConfigTopicConsumerUnitTest {

    @InjectMocks
    private ConfigTopicConsumer configTopicConsumer;
    @Mock
    private ConfigService configService;

    @Test
    public void consumeEvent() {
        configTopicConsumer.consumeEvent(new ConsumerRecord<>("config", 1, 1, "key", "{\n"
            + "    \"eventId\":\"id\",\n"
            + "    \"commit\":\"commit\",\n"
            + "    \"paths\":[\"path\"]\n"
            + "}"));

        verify(configService).updateConfigurations("commit", Collections.singleton("path"));
    }
}
