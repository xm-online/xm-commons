package com.icthh.xm.commons.topic.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DynamicConsumerConfigurationServiceUnitTest {

    private static final String TENANT_KEY = "test";

    private DynamicConsumerConfigurationService dynamicConsumerConfigurationService;

    @Mock
    private DynamicConsumerConfiguration dynamicConsumerConfiguration;

    @Mock
    private TopicManagerService topicManagerService;

    @Before
    public void setup() {
        dynamicConsumerConfigurationService = new DynamicConsumerConfigurationService(singletonList(dynamicConsumerConfiguration), topicManagerService);
    }

    @Test
    public void startDynamicConsumers() {

        when(dynamicConsumerConfiguration.getDynamicConsumers(eq(TENANT_KEY))).thenReturn();

    }

    @Test
    public void refreshDynamicConsumers() {

    }

    @Test
    public void stopDynamicConsumers() {

    }

}
