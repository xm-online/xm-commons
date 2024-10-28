package com.icthh.xm.commons.scheduler.adapter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.config.client.config.XmConfigProperties;
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

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

@RunWith(MockitoJUnitRunner.class)
public class SchedulerChannelManagerUnitTest {

    @Spy
    @InjectMocks
    SchedulerChannelManager channelManager;
    @Spy
    XmConfigProperties xmConfigProperties;
    @Mock
    DynamicTopicConsumerConfiguration dynamicTopicConsumerConfiguration;

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Before
    public void setUp() {

        MockitoAnnotations.initMocks(this);
        doNothing().when(dynamicTopicConsumerConfiguration).buildDynamicConsumers(any());
        doNothing().when(dynamicTopicConsumerConfiguration).sendRefreshDynamicConsumersEvent(any());
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
        verify(dynamicTopicConsumerConfiguration, never()).buildDynamicConsumers(any());
        verify(dynamicTopicConsumerConfiguration, never()).sendRefreshDynamicConsumersEvent(any());

    }

    @Test
    public void shouldOnlyParseConfigDuringInit() {

        String key = "/config/tenants/tenants-list.json";
        String content = readFile();

        channelManager.onInit(key, content);
        verify(channelManager, times(1)).parseConfig(eq(key), eq(content));
        verify(dynamicTopicConsumerConfiguration, never()).buildDynamicConsumers(any());
        verify(dynamicTopicConsumerConfiguration, never()).sendRefreshDynamicConsumersEvent(any());

    }

    @Test
    public void shouldStartSchedulerForActiveTenantsDuringRefresh() {

        String key = "/config/tenants/tenants-list.json";
        String content = readFile();

        SchedulerChannelManager manager = spy(new SchedulerChannelManager(xmConfigProperties,
            dynamicTopicConsumerConfiguration));

        manager.onRefresh(key, content);
        manager.onRefresh(key, content); // test multiply refresh with same content

        verify(manager, times(2)).parseConfig(key, content);

        verify(manager).startChannels();

        verify(dynamicTopicConsumerConfiguration, times(2)).buildDynamicConsumers(any());
        verify(dynamicTopicConsumerConfiguration, times(2)).sendRefreshDynamicConsumersEvent(any());

        verify(dynamicTopicConsumerConfiguration).buildDynamicConsumers(eq("xm"));
        verify(dynamicTopicConsumerConfiguration).sendRefreshDynamicConsumersEvent(eq("xm"));

        verify(dynamicTopicConsumerConfiguration).buildDynamicConsumers(eq("test"));
        verify(dynamicTopicConsumerConfiguration).sendRefreshDynamicConsumersEvent(eq("test"));

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

        SchedulerChannelManager manager = spy(new SchedulerChannelManager(xmConfigProperties,
            dynamicTopicConsumerConfiguration));

        String key = "/config/tenants/tenants-list.json";
        String content = readFile();

        manager.onRefresh(key, content);

        verify(dynamicTopicConsumerConfiguration, times(1)).buildDynamicConsumers(any());
        verify(dynamicTopicConsumerConfiguration, times(1)).sendRefreshDynamicConsumersEvent(any());

        verify(dynamicTopicConsumerConfiguration).buildDynamicConsumers(eq("xm"));
        verify(dynamicTopicConsumerConfiguration).sendRefreshDynamicConsumersEvent(eq("xm"));

    }

    @SneakyThrows
    private String readFile() {
        return new String(Files.readAllBytes(Paths.get(
            SchedulerChannelManager.class.getResource("/config/tenants/tenants-list.json").toURI())));
    }

}
