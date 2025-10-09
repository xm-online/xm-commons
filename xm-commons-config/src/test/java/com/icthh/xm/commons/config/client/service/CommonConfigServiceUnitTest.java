package com.icthh.xm.commons.config.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.config.client.api.ConfigurationChangedListener;
import com.icthh.xm.commons.config.client.api.FetchConfigurationSettings;
import com.icthh.xm.commons.config.client.repository.CommonConfigRepository;
import com.icthh.xm.commons.config.domain.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class CommonConfigServiceUnitTest {

    private CommonConfigService configService;
    @Mock
    private CommonConfigRepository commonConfigRepository;

    @Before
    public void setUp() {
        FetchConfigurationSettings fetchConfigurationSettings = new FetchConfigurationSettings("test", true);
        configService = new CommonConfigService(fetchConfigurationSettings, commonConfigRepository);
    }

    @Test
    public void getConfigurationMap() {
        Map<String, Configuration> config = Collections.singletonMap("path", new Configuration("path", "content"));
        when(commonConfigRepository.getConfig("commit")).thenReturn(config);

        assertThat(configService.getConfigurationMap("commit")).isEqualTo(config);
    }

    @Test
    public void updateConfigurations() {
        Map<String, Configuration> config = Collections.singletonMap("path", new Configuration("path", "content"));
        when(commonConfigRepository.getConfig(eq("commit"), anyList())).thenReturn(config);

        List<ConfigurationChangedListener> configurationListeners = new ArrayList<>();
        configurationListeners.add(mock(ConfigurationChangedListener.class));
        configurationListeners.add(mock(ConfigurationChangedListener.class));

        configurationListeners.forEach(configService::addConfigurationChangedListener);
        configService.updateConfigurations("commit", Collections.singletonList("path"));

        configurationListeners.forEach(configurationListener ->
                                           verify(configurationListener)
                                               .onConfigurationChanged(refEq(config.get("path"))));
        configurationListeners.forEach(configurationListener ->
                verify(configurationListener).refreshFinished(Collections.singletonList("path")));
    }

    @Test
    public void updateMultiplyConfigurationsWithSingleFinished() {
        Map<String, Configuration> config = Map.of(
                "path1", new Configuration("path1", "content1"),
                "path2", new Configuration("path2", "content2")
        );
        when(commonConfigRepository.getConfig(eq("commit"), anyList())).thenReturn(config);

        List<ConfigurationChangedListener> configurationListeners = new ArrayList<>();
        configurationListeners.add(mock(ConfigurationChangedListener.class));
        configurationListeners.add(mock(ConfigurationChangedListener.class));

        configurationListeners.forEach(configService::addConfigurationChangedListener);
        configService.updateConfigurations("commit", List.of("path1", "path2"));

        configurationListeners.forEach(configurationListener ->
                verify(configurationListener)
                        .onConfigurationChanged(refEq(config.get("path1"))));
        configurationListeners.forEach(configurationListener ->
                verify(configurationListener)
                        .onConfigurationChanged(refEq(config.get("path2"))));

        configurationListeners.forEach(configurationListener ->
                verify(configurationListener).refreshFinished(List.of("path1", "path2")));
    }

    @Test
    public void updateConfigurationsWithNullContent() {
        Map<String, Configuration> config = Collections.singletonMap("path", null);
        when(commonConfigRepository.getConfig(eq("commit"), anyList())).thenReturn(config);

        List<ConfigurationChangedListener> configurationListeners = new ArrayList<>();
        configurationListeners.add(mock(ConfigurationChangedListener.class));
        configurationListeners.add(mock(ConfigurationChangedListener.class));

        configurationListeners.forEach(configService::addConfigurationChangedListener);
        configService.updateConfigurations("commit", Collections.singletonList("path"));

        configurationListeners.forEach(configurationListener ->
                                           verify(configurationListener)
                                               .onConfigurationChanged(refEq(new Configuration("path", null))));
        configurationListeners.forEach(configurationListener ->
                verify(configurationListener).refreshFinished(Collections.singletonList("path")));
    }

}
