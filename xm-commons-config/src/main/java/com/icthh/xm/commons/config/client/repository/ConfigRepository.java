package com.icthh.xm.commons.config.client.repository;

import static com.icthh.xm.commons.config.client.utils.RequestUtils.createSimpleHeaders;

import com.icthh.xm.commons.config.client.config.XmConfigProperties;
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

    private Map<String, String> configMap;

    public Map<String, String> getMap() {
        Map<String, String> configuration = configMap;
        if (configuration == null) {
            configMap = readFromConfigService();
        }
        return configMap;
    }

    private Map<String, String> readFromConfigService() {
        ParameterizedTypeReference<Map<String, String>> typeRef = new ParameterizedTypeReference<Map<String, String>>() {
        };
        HttpEntity<String> entity = new HttpEntity<>(createSimpleHeaders());
        return restTemplate.exchange(getServiceConfigUrl(), HttpMethod.GET, entity, typeRef).getBody();
    }

    private String getServiceConfigUrl() {
        return xmConfigProperties.getXmConfigUrl() + URL;
    }
}
