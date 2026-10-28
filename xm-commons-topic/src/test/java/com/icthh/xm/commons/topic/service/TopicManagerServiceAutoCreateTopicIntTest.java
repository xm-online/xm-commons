package com.icthh.xm.commons.topic.service;

import com.icthh.xm.commons.logging.trace.TraceWrapper;
import com.icthh.xm.commons.topic.domain.TopicConfig;
import com.icthh.xm.commons.topic.message.MessageHandler;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;
import org.springframework.kafka.test.EmbeddedKafkaKraftBroker;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TopicManagerServiceAutoCreateTopicIntTest {

    private static final String TENANT_KEY = "TEST";
    private static final long TIMEOUT_MS = 10_000;

    private static EmbeddedKafkaKraftBroker broker;

    private KafkaAdmin kafkaAdmin;
    private AbstractMessageListenerContainer<?, ?> container;
    private TopicManagerService topicManager;

    @BeforeClass
    public static void startBroker() {
        broker = new EmbeddedKafkaKraftBroker(1, 1);
        broker.afterPropertiesSet();
    }

    @AfterClass
    public static void stopBroker() {
        broker.destroy();
    }

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() {
        kafkaAdmin = new KafkaAdmin(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString()));
        ObjectProvider<KafkaAdmin> kafkaAdminProvider = mock(ObjectProvider.class);
        when(kafkaAdminProvider.getIfAvailable()).thenReturn(kafkaAdmin);
        container = mock(AbstractMessageListenerContainer.class);
        topicManager = spy(new TopicManagerService(mock(ConsumerFactory.class), mock(KafkaTemplate.class),
            mock(TraceWrapper.class), kafkaAdminProvider));
        doReturn(container).when(topicManager).buildListenerContainer(any(), any(), any());
    }

    @Test
    public void testMissingTopicIsCreatedWithPartitions() {
        topicManager.processTopicConfig(TENANT_KEY, topicConfig("auto-create-missing", 16), mock(MessageHandler.class));

        assertThat(partitionCount("auto-create-missing")).isEqualTo(16);
        verify(container).start();
    }

    @Test
    public void testExistingTopicIsExtendedToPartitions() {
        broker.addTopics(new NewTopic("auto-create-existing", 1, (short) 1));
        assertThat(partitionCount("auto-create-existing")).isEqualTo(1);

        topicManager.processTopicConfig(TENANT_KEY, topicConfig("auto-create-existing", 16), mock(MessageHandler.class));

        assertThat(partitionCount("auto-create-existing")).isEqualTo(16);
        verify(container).start();
    }

    private int partitionCount(String topicName) {
        return partitionCount(topicName, System.currentTimeMillis() + TIMEOUT_MS);
    }

    private int partitionCount(String topicName, long deadline) {
        try {
            return kafkaAdmin.describeTopics(topicName).get(topicName).partitions().size();
        } catch (KafkaException e) {
            if (System.currentTimeMillis() > deadline) {
                throw e;
            }
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(100));
            return partitionCount(topicName, deadline);
        }
    }

    private TopicConfig topicConfig(String topicName, Integer partitions) {
        TopicConfig topicConfig = new TopicConfig();
        topicConfig.setKey(topicName);
        topicConfig.setTypeKey(topicName);
        topicConfig.setTopicName(topicName);
        topicConfig.setRetriesCount(1);
        topicConfig.setBackOffPeriod(100L);
        topicConfig.setAutoCreateTopic(true);
        topicConfig.setPartitions(partitions);
        return topicConfig;
    }
}
