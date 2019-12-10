package com.icthh.xm.commons.topic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.icthh.xm.commons.topic.domain.ConsumerHolder;
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
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class TopicManagerUnitTest {

    private static final String UPDATE_KEY = "/config/tenants/test/some-ms/topic-consumers.yml";
    private static final String TENANT_KEY = "test";
    private static final String APP_NAME = "some-ms";
    private static final String CONFIG_1 = "topic-consumers-1.yml";
    private static final String CONFIG_2 = "topic-consumers-2.yml";
    private static final String CONFIG_3 = "topic-consumers-3.yml";

    private TopicManager topicManager;

    @Mock
    private KafkaProperties kafkaProperties;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private KafkaConsumer kafkaConsumer;

    @Mock
    private AbstractMessageListenerContainer container;

    @Mock
    private MessageHandler messageHandler;

    @Before
    public void setUp() {
        topicManager = spy(new TopicManager(APP_NAME, kafkaProperties, kafkaTemplate, messageHandler));
        doReturn(container).when(topicManager).buildListenerContainer(any(), any());
    }

    @Test
    public void testStartingOfNewConsumers() {
        topicManager.onRefresh(UPDATE_KEY, readConfig(CONFIG_1));

        verify(container, times(3)).start();
        verify(container, times(0)).stop();

        Map<String, Map<String, ConsumerHolder>> topicConsumers = topicManager.getTenantTopicConsumers();
        assertEquals(1, topicConsumers.keySet().size());
        assertTrue(topicConsumers.containsKey(TENANT_KEY));

        Map<String, ConsumerHolder> holderMap = topicConsumers.get(TENANT_KEY);
        assertEquals(3, holderMap.keySet().size());
        assertTrue(holderMap.containsKey("key1"));
        assertTrue(holderMap.containsKey("key2"));
        assertTrue(holderMap.containsKey("key3"));
    }

    @Test
    public void testStoppingOfOldConsumer() {
        topicManager.onRefresh(UPDATE_KEY, readConfig(CONFIG_1));

        reset(container);

        topicManager.onRefresh(UPDATE_KEY, readConfig(CONFIG_2));

        verify(container, times(0)).start();
        verify(container, times(1)).stop();

        Map<String, Map<String, ConsumerHolder>> topicConsumers = topicManager.getTenantTopicConsumers();
        assertEquals(1, topicConsumers.keySet().size());
        assertTrue(topicConsumers.containsKey(TENANT_KEY));

        Map<String, ConsumerHolder> holderMap = topicConsumers.get(TENANT_KEY);
        assertEquals(2, holderMap.keySet().size());
        assertTrue(holderMap.containsKey("key1"));
        assertTrue(holderMap.containsKey("key2"));
    }

    @Test
    public void testUpdatingOfConsumer() {
        topicManager.onRefresh(UPDATE_KEY, readConfig(CONFIG_1));

        reset(container);

        topicManager.onRefresh(UPDATE_KEY, readConfig(CONFIG_3));

        verify(container, times(1)).stop();
        verify(container, times(1)).start();

        Map<String, Map<String, ConsumerHolder>> topicConsumers = topicManager.getTenantTopicConsumers();
        assertEquals(1, topicConsumers.keySet().size());
        assertTrue(topicConsumers.containsKey(TENANT_KEY));

        Map<String, ConsumerHolder> holderMap = topicConsumers.get(TENANT_KEY);
        assertEquals(3, holderMap.keySet().size());
        assertTrue(holderMap.containsKey("key1"));
        assertTrue(holderMap.containsKey("key2"));
        assertTrue(holderMap.containsKey("key3"));

        ConsumerHolder consumerHolder = holderMap.get("key2");
        assertEquals(new Integer(2), consumerHolder.getTopicConfig().getRetriesCount());
    }

    @Test
    public void testConfigurationWasRemoved() {
        topicManager.onRefresh(UPDATE_KEY, readConfig(CONFIG_1));

        reset(container);

        topicManager.onRefresh(UPDATE_KEY, null);

        verify(container, times(3)).stop();
        verify(container, times(0)).start();

        Map<String, Map<String, ConsumerHolder>> topicConsumers = topicManager.getTenantTopicConsumers();
        assertTrue(topicConsumers.isEmpty());
    }

    @Test
    public void testConfigurationTheSame() {
        topicManager.onRefresh(UPDATE_KEY, readConfig(CONFIG_1));

        reset(container);

        topicManager.onRefresh(UPDATE_KEY, readConfig(CONFIG_1));

        verifyNoMoreInteractions(container);
    }

    @SneakyThrows
    private String readConfig(String name) {
        return IOUtils.toString(this.getClass().getResourceAsStream("/config/" + name), Charset.defaultCharset());
    }
}

