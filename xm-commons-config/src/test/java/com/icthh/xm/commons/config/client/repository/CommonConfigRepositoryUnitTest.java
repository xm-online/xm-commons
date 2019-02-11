package com.icthh.xm.commons.config.client.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import com.icthh.xm.commons.config.client.config.XmConfigProperties;
import com.icthh.xm.commons.config.domain.Configuration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class CommonConfigRepositoryUnitTest {

    @InjectMocks
    private CommonConfigRepository configRepository;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private XmConfigProperties xmConfigProperties;

    @Test
    public void getConfig() {
        Map<String, Configuration> config = Collections
            .singletonMap("path", new Configuration("path", "content"));
        when(xmConfigProperties.getXmConfigUrl()).thenReturn("configUrl");
        when(restTemplate.exchange(eq("configUrl/api/private/config_map?version=commit"), eq(HttpMethod.GET), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
            .thenReturn(ResponseEntity.ok(config));

        assertThat(configRepository.getConfig("commit")).isEqualTo(config);
    }

    @Test
    public void updateConfig() {
        when(xmConfigProperties.getXmConfigUrl()).thenReturn("configUrl");

        configRepository.updateConfigFullPath(new Configuration("path", "content"), "hash");

        HttpEntity<Configuration> expected = new HttpEntity<>(new Configuration("path", "content"));
        verify(restTemplate).exchange(eq("configUrl/api/private/config?oldConfigHash=hash"), eq(HttpMethod.PUT), refEq(expected), eq(Void.class));
    }
}
