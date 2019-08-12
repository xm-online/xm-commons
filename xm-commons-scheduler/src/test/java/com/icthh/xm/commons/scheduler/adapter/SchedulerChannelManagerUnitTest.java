package com.icthh.xm.commons.scheduler.adapter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.cloud.stream.binder.kafka.properties.KafkaConsumerProperties.StartOffset.earliest;
import static org.springframework.cloud.stream.binder.kafka.properties.KafkaConsumerProperties.StartOffset.latest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.config.client.config.XmConfigProperties;
import com.icthh.xm.commons.scheduler.service.SchedulerEventService;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.actuate.health.CompositeHealthIndicator;
import org.springframework.cloud.stream.binder.kafka.KafkaBinderHealthIndicator;
import org.springframework.cloud.stream.binder.kafka.KafkaMessageChannelBinder;
import org.springframework.cloud.stream.binding.BindingService;
import org.springframework.cloud.stream.binding.SubscribableChannelBindingTargetFactory;
import org.springframework.cloud.stream.config.BindingServiceProperties;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

@RunWith(MockitoJUnitRunner.class)
public class SchedulerChannelManagerUnitTest {

    @Spy
    @InjectMocks
    SchedulerChannelManager channelManager;

    @Mock
    BindingServiceProperties bindingServiceProperties;
    @Mock
    SubscribableChannelBindingTargetFactory bindingTargetFactory;
    @Mock
    BindingService bindingService;
    @Mock
    KafkaMessageChannelBinder kafkaMessageChannelBinder;
    @Spy
    ObjectMapper objectMapper;
    @Mock
    SchedulerEventService schedulerEventService;
    @Mock
    CompositeHealthIndicator bindersHealthIndicator;
    @Mock
    KafkaBinderHealthIndicator kafkaBinderHealthIndicator;
    @Spy
    XmConfigProperties xmConfigProperties;

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Before
    public void setUp() {

        MockitoAnnotations.initMocks(this);
        doNothing().when(channelManager).createHandler(any(), any(), any(), any());
        channelManager.appName = "entity";

    }

    @Test
    public void shouldFailTenantListNotFound() {

        expectedEx.expect(IllegalArgumentException.class);
        expectedEx.expectMessage("Wrong config key to update /config/wrong/key");

        channelManager.onInit("/config/wrong/key", "");

    }

    @Test
    public void shouldFailTenantListIsEmpty() {

        expectedEx.expect(IllegalArgumentException.class);
        expectedEx.expectMessage("Config file has empty content: /config/tenants/tenants-list.json");

        channelManager.onInit("/config/tenants/tenants-list.json", "");

    }

    @Test
    public void shouldNotStartAnyWhenNoTenantsInLIst() {

        channelManager.onInit("/config/tenants/tenants-list.json", "{}");
        verify(channelManager, never()).createChannels(any());

    }

    @Test
    public void shouldOnlyParseConfigDuringInit() {

        String key = "/config/tenants/tenants-list.json";
        String content = readFile();

        channelManager.onInit(key, content);
        verify(channelManager, times(1)).parseConfig(eq(key), eq(content));
        verify(channelManager, never()).createChannels(any());

    }

    @Test
    public void shouldStartSchedulerForActiveTenantsDuringRefresh() {

        String key = "/config/tenants/tenants-list.json";
        String content = readFile();

        channelManager.onRefresh(key, content);

        verify(channelManager).parseConfig(key, content);

        verify(channelManager).startChannels();

        verify(channelManager, times(2)).createChannels(any());

        verify(channelManager).createChannels(eq("xm"));

        verify(channelManager, times(4)).createHandler(anyString(), anyString(), eq("XM"), any());
        verify(channelManager).createHandler(eq("scheduler_xm_queue"), eq("GENERALGROUP"), eq("XM"), eq(earliest));
        verify(channelManager).createHandler(eq("scheduler_xm_topic"), anyString(), eq("XM"), eq(latest));
        verify(channelManager).createHandler(eq("scheduler_xm_entity_queue"), eq("entity"), eq("XM"), eq(earliest));
        verify(channelManager).createHandler(eq("scheduler_xm_entity_topic"), anyString(), eq("XM"), eq(latest));

        verify(channelManager).createChannels(eq("test"));
        verify(channelManager, times(4)).createHandler(anyString(), anyString(), eq("TEST"), any());
        verify(channelManager).createHandler(eq("scheduler_test_queue"), eq("GENERALGROUP"), eq("TEST"), eq(earliest));
        verify(channelManager).createHandler(eq("scheduler_test_topic"), anyString(), eq("TEST"), eq(latest));
        verify(channelManager).createHandler(eq("scheduler_test_entity_queue"), eq("entity"), eq("TEST"), eq(earliest));
        verify(channelManager).createHandler(eq("scheduler_test_entity_topic"), anyString(), eq("TEST"), eq(latest));

    }

    @Test
    public void shouldFailIfStartCalledBeforeInit() {

        expectedEx.expect(IllegalStateException.class);
        expectedEx.expectMessage("Scheduler channel manager was not initialized. Call onInit() first!");

        channelManager.startChannels();

    }

    @Test
    public void shouldStartSchedulerForActiveAndIncludedTenantsDuringRefresh() {

        Set<String> included = new HashSet<>();
        included.add("XM");
        included.add("demo"); // will not start scheduler as it is not configured in tenants-list.json

        when(xmConfigProperties.getIncludeTenants()).thenReturn(included);

        SchedulerChannelManager manager = spy(new SchedulerChannelManager(bindingServiceProperties,
                                                                          bindingTargetFactory,
                                                                          bindingService,
                                                                          kafkaMessageChannelBinder,
                                                                          objectMapper,
                                                                          schedulerEventService,
                                                                          bindersHealthIndicator,
                                                                          kafkaBinderHealthIndicator,
                                                                          xmConfigProperties));

        doNothing().when(manager).createHandler(any(), any(), any(), any());

        String key = "/config/tenants/tenants-list.json";
        String content = readFile();

        manager.onRefresh(key, content);

        verify(manager, times(1)).createChannels(any());

        verify(manager).createChannels(eq("xm"));

    }

    @SneakyThrows
    private String readFile() {
        return new String(Files.readAllBytes(Paths.get(
            SchedulerChannelManager.class.getResource("/config/tenants/tenants-list.json").toURI())));
    }

}
