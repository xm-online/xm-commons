package com.icthh.xm.commons.scheduler.adapter;

import com.icthh.xm.commons.scheduler.service.SchedulerEventHandlerFacade;
import com.icthh.xm.commons.scheduler.service.SchedulerEventService;
import com.icthh.xm.commons.topic.domain.DynamicConsumer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class DynamicTopicConsumerConfigurationUnitTest {

    @InjectMocks
    DynamicTopicConsumerConfiguration dynamicTopicConsumerConfiguration;

    @Mock
    SchedulerEventService schedulerEventService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldReturnEmptyListForTenant() {
        List<DynamicConsumer> result = dynamicTopicConsumerConfiguration.getDynamicConsumers("TEST");

        assertThat(result).isNotNull();
        assertThat(result.size()).isZero();
    }

    @Test
    public void shouldBuildDynamicConsumerForActiveTenant() {
        ReflectionTestUtils.setField(dynamicTopicConsumerConfiguration, "appName", "entity");
        ReflectionTestUtils.setField(dynamicTopicConsumerConfiguration, "backOffInitialInterval", 11111);
        ReflectionTestUtils.setField(dynamicTopicConsumerConfiguration, "backOffMaxInterval", 22222);
        ReflectionTestUtils.setField(dynamicTopicConsumerConfiguration, "kafkaMetadataMaxAge", 33333);

        String tenantName = "XM";

        assertThat(dynamicTopicConsumerConfiguration.getDynamicConsumers(tenantName).size()).isZero();

        dynamicTopicConsumerConfiguration.buildDynamicConsumers(tenantName);

        List<DynamicConsumer> result = dynamicTopicConsumerConfiguration.getDynamicConsumers(tenantName);

        assertThat(result.size()).isEqualTo(4);

        assertThatTopicConfigEqual(result.get(0), "scheduler_xm_queue", "GENERALGROUP", "earliest");
        assertThatTopicConfigEqual(result.get(1), "scheduler_xm_topic", null, "latest");
        assertThatTopicConfigEqual(result.get(2), "scheduler_xm_entity_queue", "entity", "earliest");
        assertThatTopicConfigEqual(result.get(3), "scheduler_xm_entity_topic", null, "latest");
    }

    private void assertThatTopicConfigEqual(DynamicConsumer resultConsumer, String topicName, String group, String offset) {
        assertThat(resultConsumer.getConfig().getKey()).isEqualTo(topicName);
        assertThat(resultConsumer.getConfig().getTypeKey()).isEqualTo(topicName);
        assertThat(resultConsumer.getConfig().getTopicName()).isEqualTo(topicName);
        assertThat(resultConsumer.getConfig().getRetriesCount()).isEqualTo(Integer.MAX_VALUE);
        assertThat(resultConsumer.getConfig().getBackOffPeriod()).isEqualTo(11111);
        assertThat(resultConsumer.getConfig().getMaxPollInterval()).isEqualTo(22222);
        if (group != null) {
            assertThat(resultConsumer.getConfig().getGroupId()).isEqualTo(group);
        } else {
            assertThat(resultConsumer.getConfig().getGroupId()).isNotBlank();
        }
        assertThat(resultConsumer.getConfig().getAutoOffsetReset()).isEqualTo(offset);
        assertThat(resultConsumer.getConfig().getMetadataMaxAge()).isEqualTo("33333");
        assertThat(resultConsumer.getMessageHandler().getClass()).isEqualTo(SchedulerEventHandlerFacade.class);
    }
}
