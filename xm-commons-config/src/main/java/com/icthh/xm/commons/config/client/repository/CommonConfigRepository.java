package com.icthh.xm.commons.config.client.repository;

import static com.icthh.xm.commons.config.client.repository.TenantConfigRepository.OLD_CONFIG_HASH;
import static com.icthh.xm.commons.config.client.utils.RequestUtils.createApplicationJsonHeaders;
import static com.icthh.xm.commons.config.client.utils.RequestUtils.createSimpleHeaders;

import com.icthh.xm.commons.config.client.config.XmConfigProperties;
import com.icthh.xm.commons.config.domain.Configuration;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collection;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class CommonConfigRepository {

    private static final String URL = "/api/private";
    private static final String VERSION = "version";
    private final RestTemplate restTemplate;
    private final XmConfigProperties xmConfigProperties;

    public Map<String, Configuration> getConfig(String commit) {
        ParameterizedTypeReference<Map<String, Configuration>> typeRef = new ParameterizedTypeReference<Map<String, Configuration>>() {};
        HttpEntity<String> entity = new HttpEntity<>(createSimpleHeaders());
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(getServiceConfigUrl() + "/config_map").queryParam(VERSION, commit);
        return restTemplate.exchange(builder.toUriString(), HttpMethod.GET, entity, typeRef).getBody();
    }

    private String getServiceConfigUrl() {
        return xmConfigProperties.getXmConfigUrl() + URL;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class GetConfigRequest {
        private String version;
        private Collection<String> paths;
    }

    public Map<String,Configuration> getConfig(String version, Collection<String> paths) {
        ParameterizedTypeReference<Map<String, Configuration>> typeRef = new ParameterizedTypeReference<Map<String, Configuration>>() {};
        HttpEntity<GetConfigRequest> entity = new HttpEntity<>(new GetConfigRequest(version, paths), createApplicationJsonHeaders());
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(getServiceConfigUrl() + "/config_map");
        return restTemplate.exchange(builder.toUriString(), HttpMethod.POST, entity, typeRef).getBody();
    }

    public void updateConfigFullPath(Configuration configuration, String oldConfigHash) {
        HttpEntity<Configuration> entity = new HttpEntity<>(configuration);
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(getServiceConfigUrl() + "/config")
            .queryParam(OLD_CONFIG_HASH, oldConfigHash);
        restTemplate.exchange(builder.toUriString(), HttpMethod.PUT, entity, Void.class);
    }

}
