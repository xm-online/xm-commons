package com.icthh.xm.commons.topic.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.logging.trace.SleuthWrapper;
import com.icthh.xm.commons.topic.domain.ConsumerHolder;
import com.icthh.xm.commons.topic.domain.TopicConfig;
import com.icthh.xm.commons.topic.domain.TopicConsumersSpec;
import com.icthh.xm.commons.topic.message.MessageHandler;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class TopicManagerServiceUnitTest {

    private static final String TENANT_KEY = "test";
    private static final String CONFIG_1 = "topic-consumers-1.yml";
    private static final String CONFIG_2 = "topic-consumers-2.yml";
    private static final String CONFIG_3 = "topic-consumers-3.yml";

    private TopicManagerService topicManager;

    @Mock
    private KafkaProperties kafkaProperties;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private AbstractMessageListenerContainer container;

    @Mock
    private MessageHandler messageHandler;

    @Mock
    private SleuthWrapper sleuthWrapper;


    @Before
    public void setUp() {
        topicManager = spy(new TopicManagerService(kafkaProperties, kafkaTemplate, sleuthWrapper));
        doReturn(container).when(topicManager).buildListenerContainer(any(), any(), any());
    }

    @Test
    public void testStartingOfNewConsumers() {
        HashMap<String, ConsumerHolder> existingConsumers = new HashMap<>();
        TopicConfig topicConfig = getTopicConsumerSpec(readConfig(CONFIG_1)).getTopics().get(0);
        topicManager.processTopicConfig(TENANT_KEY, topicConfig, existingConsumers, messageHandler);

        verify(container, times(1)).start();
        verify(container, times(0)).stop();

        assertEquals(1, existingConsumers.keySet().size());
        assertTrue(existingConsumers.containsKey("key1"));

        verifyNoMoreInteractions(container);
    }

    @Test
    public void testStoppingOfOldConsumer() {
        HashMap<String, ConsumerHolder> existingConsumers = new HashMap<>();
        TopicConfig firstTopicConfig = getTopicConsumerSpec(readConfig(CONFIG_1)).getTopics().get(0);
        TopicConfig secondTopicConfig = getTopicConsumerSpec(readConfig(CONFIG_2)).getTopics().get(1);
        topicManager.processTopicConfig(TENANT_KEY, firstTopicConfig, existingConsumers, messageHandler);
        topicManager.processTopicConfig(TENANT_KEY, secondTopicConfig, existingConsumers, messageHandler);

        reset(container);

        topicManager.removeOldConsumers(TENANT_KEY, List.of(secondTopicConfig), existingConsumers);

        verify(container, times(0)).start();
        verify(container, times(1)).stop();

        assertEquals(1, existingConsumers.keySet().size());
        assertTrue(existingConsumers.containsKey("key2"));

        verifyNoMoreInteractions(container);
    }

    @Test
    public void testUpdatingOfConsumer() {
        HashMap<String, ConsumerHolder> existingConsumers = new HashMap<>();
        TopicConfig firstTopicConfig = getTopicConsumerSpec(readConfig(CONFIG_1)).getTopics().get(0);
        TopicConfig thirdTopicConfig = getTopicConsumerSpec(readConfig(CONFIG_3)).getTopics().get(0);
        topicManager.processTopicConfig(TENANT_KEY, firstTopicConfig, existingConsumers, messageHandler);

        reset(container);

        topicManager.processTopicConfig(TENANT_KEY, thirdTopicConfig, existingConsumers, messageHandler);

        verify(container, times(1)).stop();
        verify(container, times(1)).start();

        assertEquals(1, existingConsumers.keySet().size());
        assertTrue(existingConsumers.containsKey("key1"));

        ConsumerHolder consumerHolder = existingConsumers.get("key1");
        assertEquals(5, (int) consumerHolder.getTopicConfig().getRetriesCount());

        verifyNoMoreInteractions(container);
    }

    @Test
    public void testConfigurationTheSame() {
        HashMap<String, ConsumerHolder> existingConsumers = new HashMap<>();
        TopicConfig topicConfig = getTopicConsumerSpec(readConfig(CONFIG_1)).getTopics().get(0);
        topicManager.processTopicConfig(TENANT_KEY, topicConfig, existingConsumers, messageHandler);

        reset(container);

        topicManager.processTopicConfig(TENANT_KEY, topicConfig, existingConsumers, messageHandler);

        verifyNoMoreInteractions(container);
    }

    @SneakyThrows
    private String readConfig(String name) {
        return IOUtils.toString(this.getClass().getResourceAsStream("/config/" + name), Charset.defaultCharset());
    }

    @SneakyThrows
    private TopicConsumersSpec getTopicConsumerSpec(String content) {
        return new ObjectMapper(new YAMLFactory()).readValue(content, TopicConsumersSpec.class);
    }
}

