package com.icthh.xm.commons.topic.config;

import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.internal.DefaultTenantContextHolder;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class KafkaTopicNameHandlerUnitTest {

    private KafkaTopicNameHandler kafkaTopicNameHandler;
    private TenantContextHolder cth = new DefaultTenantContextHolder();

    @Before
    public void setUp() {
        TenantContextUtils.setTenant(cth, "TEST");
        kafkaTopicNameHandler = new KafkaTopicNameHandler();
    }

    @Test
    public void testGetPrefixedTopicNameWithTenantPrefixEnabled() {
        kafkaTopicNameHandler.setAddTenantPrefix(true);

        String prefixedTopic = kafkaTopicNameHandler.getPrefixedTopicName("my-topic", "tenant1");

        assertEquals("tenant_topic_tenant1_my-topic", prefixedTopic);
    }

    @Test
    public void testGetPrefixedTopicNameWithTenantPrefixDisabled() {
        kafkaTopicNameHandler.setAddTenantPrefix(false);

        String prefixedTopic = kafkaTopicNameHandler.getPrefixedTopicName("my-topic", "tenant1");

        assertEquals("my-topic", prefixedTopic);
    }

    @Test
    public void testGetPrefixedTopicNameWithNullTenant() {
        kafkaTopicNameHandler.setAddTenantPrefix(true);

        String prefixedTopic = kafkaTopicNameHandler.getPrefixedTopicName("my-topic", null);

        assertEquals("my-topic", prefixedTopic);
    }

    @Test
    public void testGetPrefixedTopicNameWithEmptyTenant() {
        kafkaTopicNameHandler.setAddTenantPrefix(true);

        String prefixedTopic = kafkaTopicNameHandler.getPrefixedTopicName("my-topic", "");

        assertEquals("my-topic", prefixedTopic);
    }

    @Test
    public void testGetPrefixedTopicNameWithNullTopic() {
        kafkaTopicNameHandler.setAddTenantPrefix(true);

        String prefixedTopic = kafkaTopicNameHandler.getPrefixedTopicName(null, "tenant1");

        assertNull(prefixedTopic);
    }

    @Test
    public void testGetPrefixedTopicNameWithSystemQueue() {
        kafkaTopicNameHandler.setAddTenantPrefix(true);

        String prefixedTopic = kafkaTopicNameHandler.getPrefixedTopicName("system_queue", "xm");

        assertEquals("tenant_topic_xm_system_queue", prefixedTopic);
    }

    @Test
    public void testGetPrefixedTopicNameWithConfigTopic() {
        kafkaTopicNameHandler.setAddTenantPrefix(true);

        String prefixedTopic = kafkaTopicNameHandler.getPrefixedTopicName("config_topic", "demo");

        // Config topics should NOT be prefixed
        assertEquals("config_topic", prefixedTopic);
    }

    @Test
    public void testGetPrefixedTopicNameWithConfigTopicStartingWithConfig() {
        kafkaTopicNameHandler.setAddTenantPrefix(true);

        String prefixedTopic = kafkaTopicNameHandler.getPrefixedTopicName("config_queue", "demo");

        // Config topics should NOT be prefixed
        assertEquals("config_queue", prefixedTopic);
    }

    @Test
    public void testGetPrefixedTopicNameWithDynamicTopic() {
        kafkaTopicNameHandler.setAddTenantPrefix(true);

        String prefixedTopic = kafkaTopicNameHandler.getPrefixedTopicName("event.tenant1.entity", "tenant1");

        assertEquals("tenant_topic_tenant1_event.tenant1.entity", prefixedTopic);
    }
}
