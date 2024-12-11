package com.icthh.xm.commons.topic.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.topic.domain.DynamicConsumer;
import com.icthh.xm.commons.topic.domain.TopicConsumersSpec;
import com.icthh.xm.commons.topic.message.MessageHandler;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationEventPublisher;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class TopicConfigurationServiceUnitTest {

    private static final String UPDATE_KEY = "/config/tenants/test/some-ms/topic-consumers.yml";
    private static final String TENANT_KEY = "test";
    private static final String APP_NAME = "some-ms";
    private static final String CONFIG_1 = "topic-consumers-1.yml";

    private TopicConfigurationService topicConfigurationService;
    private TopicDynamicConsumerConfiguration topicDynamicConsumerConfiguration;

    @Mock
    private DynamicConsumerConfigurationService dynamicConsumerConfigurationService;
    @Mock
    private MessageHandler messageHandler;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Before
    public void setUp() {
        topicDynamicConsumerConfiguration = spy(new TopicDynamicConsumerConfiguration(applicationEventPublisher, messageHandler));
        topicConfigurationService = spy(new TopicConfigurationService(APP_NAME, topicDynamicConsumerConfiguration));
    }

    @Test
    public void testOnRefresh() {
        String content = readConfig(CONFIG_1);
        TopicConsumersSpec topicConsumerSpec = getTopicConsumerSpec(content);

        topicConfigurationService.onRefresh(UPDATE_KEY, content);

        Map<String, List<DynamicConsumer>> topicConsumers = topicConfigurationService.getTenantTopicConsumers();
        List<DynamicConsumer> dynamicConsumers = topicConsumers.get(TENANT_KEY);
        dynamicConsumers.forEach(dynamicConsumer -> assertTrue(topicConsumerSpec.getTopics().stream().anyMatch(topicConfig -> topicConfig.getKey().equals(dynamicConsumer.getConfig().getKey()))));

        verify(topicDynamicConsumerConfiguration).sendRefreshDynamicConsumersEvent(eq(TENANT_KEY));
        verifyNoMoreInteractions(dynamicConsumerConfigurationService);
    }

    @Test
    public void testOnRefreshWhenConfigWasRemoved() {
        topicConfigurationService.onRefresh(UPDATE_KEY, readConfig(CONFIG_1));
        topicConfigurationService.onRefresh(UPDATE_KEY, null);

        Map<String, List<DynamicConsumer>> topicConsumers = topicConfigurationService.getTenantTopicConsumers();
        assertTrue(topicConsumers.isEmpty());

        verify(topicDynamicConsumerConfiguration, times(2)).sendRefreshDynamicConsumersEvent(eq(TENANT_KEY));
        verifyNoMoreInteractions(dynamicConsumerConfigurationService);
    }

    @Test
    public void testOnRefreshWhenSpecNull() {
        Map<String, List<DynamicConsumer>> topicConsumers = topicConfigurationService.getTenantTopicConsumers();
        int sizeBeforeRefresh = topicConsumers.size();

        topicConfigurationService.onRefresh(UPDATE_KEY, "--- \n");

        assertEquals(sizeBeforeRefresh, topicConsumers.size());

        verify(topicDynamicConsumerConfiguration).sendRefreshDynamicConsumersEvent(eq(TENANT_KEY));
        verifyNoMoreInteractions(dynamicConsumerConfigurationService);
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
