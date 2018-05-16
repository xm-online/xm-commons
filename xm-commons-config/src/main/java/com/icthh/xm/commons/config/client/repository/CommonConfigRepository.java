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
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class CommonConfigRepository {

    private static final String URL = "/api/config_map";
    private static final String COMMIT = "commit";
    private final RestTemplate restTemplate;
    private final XmConfigProperties xmConfigProperties;

    public Map<String, Configuration> getConfig(String commit) {
        ParameterizedTypeReference<Map<String, Configuration>> typeRef = new ParameterizedTypeReference<Map<String, Configuration>>() {
        };
        HttpEntity<String> entity = new HttpEntity<>(createSimpleHeaders());
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(getServiceConfigUrl())
            .queryParam(COMMIT, commit);
        return restTemplate.exchange(builder.toUriString(), HttpMethod.GET, entity, typeRef).getBody();
    }

    private String getServiceConfigUrl() {
        return xmConfigProperties.getXmConfigUrl() + URL;
    }
}
