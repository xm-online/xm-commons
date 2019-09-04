package com.icthh.xm.commons.config.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.config.client.api.ConfigurationChangedListener;
import com.icthh.xm.commons.config.client.repository.CommonConfigRepository;
import com.icthh.xm.commons.config.domain.Configuration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class CommonConfigServiceUnitTest {

    @InjectMocks
    private CommonConfigService configService;
    @Mock
    private CommonConfigRepository commonConfigRepository;

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
    }

}
