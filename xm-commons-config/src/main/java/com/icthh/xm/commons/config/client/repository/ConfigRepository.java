package com.icthh.xm.commons.config.client.repository;

import static com.icthh.xm.commons.config.client.utils.RequestUtils.createAuthHeaders;
import static com.icthh.xm.commons.config.client.utils.RequestUtils.createSimpleHeaders;

import com.icthh.xm.commons.config.client.config.XmConfigProperties;
import com.icthh.xm.commons.config.domain.ConfigurationFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class ConfigRepository {

    private static final String URL = "/api/config/";
    private final RestTemplate restTemplate;
    private final XmConfigProperties xmConfigProperties;

    private Map<String, String> configMap;

    public Map<String, String> getMap() {
        Map<String, String> configuration = configMap;
        if (configuration == null) {
            configMap = readFromConfigService().stream()
                .collect(Collectors.toMap(e -> e.getPath(), e -> e.getContent()));
        }
        return configMap;
    }

    private List<ConfigurationFile> readFromConfigService() {
        ParameterizedTypeReference<List<ConfigurationFile>> typeRef = new ParameterizedTypeReference<List<ConfigurationFile>>() {
        };
        HttpEntity<String> entity = new HttpEntity<>(createSimpleHeaders());
        return restTemplate.exchange(getServiceConfigUrl(), HttpMethod.GET, entity, typeRef).getBody();
    }

    private String getServiceConfigUrl() {
        return xmConfigProperties.getXmConfigUrl() + URL;
    }
}
