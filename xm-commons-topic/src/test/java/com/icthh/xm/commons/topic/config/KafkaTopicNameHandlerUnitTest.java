package com.icthh.xm.commons.topic.config;

import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.internal.DefaultTenantContextHolder;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class KafkaTopicNameHandlerUnitTest {

    private KafkaTopicNameHandler kafkaTopicNameHandler;
    private TenantContextHolder cth = new DefaultTenantContextHolder();

    @Before
    public void setUp() {
        TenantContextUtils.setTenant(cth, "TEST");
        kafkaTopicNameHandler = new KafkaTopicNameHandler();
        ReflectionTestUtils.setField(kafkaTopicNameHandler, "addTenantPrefix", true);
    }

    @Test
    public void testGetPrefixedTopicNameWithTenantPrefixEnabled() {
        String prefixedTopic = kafkaTopicNameHandler.getPrefixedTopicName("my-topic", "tenant1");

        assertEquals("tenant_topic_tenant1_my-topic", prefixedTopic);
    }

    @Test
    public void testGetPrefixedTopicNameWithTenantPrefixDisabled() {
        ReflectionTestUtils.setField(kafkaTopicNameHandler, "addTenantPrefix", false);

        String prefixedTopic = kafkaTopicNameHandler.getPrefixedTopicName("my-topic", "tenant1");

        assertEquals("my-topic", prefixedTopic);
    }

    @Test
    public void testGetPrefixedTopicNameWithNullTenant() {
        String prefixedTopic = kafkaTopicNameHandler.getPrefixedTopicName("my-topic", null);

        assertEquals("my-topic", prefixedTopic);
    }

    @Test
    public void testGetPrefixedTopicNameWithEmptyTenant() {
        String prefixedTopic = kafkaTopicNameHandler.getPrefixedTopicName("my-topic", "");

        assertEquals("my-topic", prefixedTopic);
    }

    @Test
    public void testGetPrefixedTopicNameWithNullTopic() {
        String prefixedTopic = kafkaTopicNameHandler.getPrefixedTopicName(null, "tenant1");

        assertNull(prefixedTopic);
    }

    @Test
    public void testGetPrefixedTopicNameWithSystemQueue() {
        String prefixedTopic = kafkaTopicNameHandler.getPrefixedTopicName("system_queue", "xm");

        assertEquals("tenant_topic_xm_system_queue", prefixedTopic);
    }

    @Test
    public void testGetPrefixedTopicNameWithSystemTopic() {
        String prefixedTopic = kafkaTopicNameHandler.getPrefixedTopicName("system-topic", "tenant1");

        assertEquals("tenant_topic_tenant1_system-topic", prefixedTopic);
    }

    @Test
    public void testGetPrefixedTopicNameWithDynamicTopic() {
        String prefixedTopic = kafkaTopicNameHandler.getPrefixedTopicName("event.tenant1.entity", "tenant1");

        assertEquals("tenant_topic_tenant1_event.tenant1.entity", prefixedTopic);
    }
}
