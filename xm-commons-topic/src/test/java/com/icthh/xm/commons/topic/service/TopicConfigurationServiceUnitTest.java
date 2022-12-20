package com.icthh.xm.commons.topic.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.topic.domain.ConsumerHolder;
import com.icthh.xm.commons.topic.domain.TopicConfig;
import com.icthh.xm.commons.topic.domain.TopicConsumersSpec;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.nio.charset.Charset;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

@RunWith(MockitoJUnitRunner.class)
public class TopicConfigurationServiceUnitTest {

    private static final String UPDATE_KEY = "/config/tenants/test/some-ms/topic-consumers.yml";
    private static final String TENANT_KEY = "test";
    private static final String APP_NAME = "some-ms";
    private static final String CONFIG_1 = "topic-consumers-1.yml";

    private TopicConfigurationService topicConfigurationService;

    @Mock
    private TopicManagerService topicManagerService;

    @Mock
    private DynamicConsumerConfigurationService dynamicConsumerConfigurationService;

    @Before
    public void setUp() {
        topicConfigurationService = spy(new TopicConfigurationService(APP_NAME, topicManagerService, dynamicConsumerConfigurationService));
    }

    @Test
    public void testOnRefresh() {
        String content = readConfig(CONFIG_1);
        TopicConsumersSpec topicConsumerSpec = getTopicConsumerSpec(content);

        topicConfigurationService.onRefresh(UPDATE_KEY, content);

        verify(topicManagerService, times(3)).processTopicConfig(eq(TENANT_KEY), isExpectedTopicConfig(), eq(emptyMap()));
        verify(topicManagerService).removeOldConsumers(eq(TENANT_KEY), eq(topicConsumerSpec.getTopics()), eq(emptyMap()));
    }

    @Test
    public void testOnRefreshWhenConfigWasRemoved() {
        topicConfigurationService.onRefresh(UPDATE_KEY, readConfig(CONFIG_1));
        topicConfigurationService.onRefresh(UPDATE_KEY, null);

        Map<String, Map<String, ConsumerHolder>> topicConsumers = topicConfigurationService.getTenantTopicConsumers();
        assertTrue(topicConsumers.isEmpty());

        verify(topicManagerService).stopAllTenantConsumers(eq(TENANT_KEY), eq(emptyMap()));
    }

    @Test
    public void testOnRefreshWhenSpecNull() {
        topicConfigurationService.onRefresh(UPDATE_KEY, "--- \n");

        verifyZeroInteractions(topicManagerService);
    }

    @SneakyThrows
    private String readConfig(String name) {
        return IOUtils.toString(this.getClass().getResourceAsStream("/config/" + name), Charset.defaultCharset());
    }

    @SneakyThrows
    private TopicConsumersSpec getTopicConsumerSpec(String content) {
        return new ObjectMapper(new YAMLFactory()).readValue(content, TopicConsumersSpec.class);
    }

    private TopicConfig isExpectedTopicConfig() {
        return argThat((TopicConfig config) -> config.getKey().equals("key1")
            || config.getKey().equals("key2")
            || config.getKey().equals("key3"));
    }
}

