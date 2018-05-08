package com.icthh.xm.commons.config.client.repository;

import static com.icthh.xm.commons.config.client.utils.RequestUtils.createSimpleHeaders;

import com.icthh.xm.commons.config.client.config.XmConfigProperties;
import com.icthh.xm.commons.config.domain.Configuration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class ConfigRepository {

    private static final String URL = "/api/config_map";
    private final RestTemplate restTemplate;
    private final XmConfigProperties xmConfigProperties;

    private Map<String, Configuration> configMap;

    public Map<String, Configuration> getMap() {
        Map<String, Configuration> configuration = configMap;
        if (configuration == null) {
            configMap = readFromConfigService();
        }
        return configMap;
    }

    private Map<String, Configuration> readFromConfigService() {
        ParameterizedTypeReference<Map<String, Configuration>> typeRef = new ParameterizedTypeReference<Map<String, Configuration>>() {
        };
        HttpEntity<String> entity = new HttpEntity<>(createSimpleHeaders());
        return restTemplate.exchange(getServiceConfigUrl(), HttpMethod.GET, entity, typeRef).getBody();
    }

    private String getServiceConfigUrl() {
        return xmConfigProperties.getXmConfigUrl() + URL;
    }
}
