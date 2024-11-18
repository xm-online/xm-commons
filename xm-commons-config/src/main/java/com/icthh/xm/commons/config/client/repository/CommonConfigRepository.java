package com.icthh.xm.commons.config.client.repository;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.icthh.xm.commons.config.client.config.XmConfigProperties;
import com.icthh.xm.commons.config.client.repository.message.ConfigurationUpdateMessage;
import com.icthh.xm.commons.config.client.repository.message.GetConfigRequest;
import com.icthh.xm.commons.config.domain.ConfigQueueEvent;
import com.icthh.xm.commons.config.domain.Configuration;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import static com.icthh.xm.commons.config.client.utils.RequestUtils.createApplicationJsonHeaders;
import static com.icthh.xm.commons.config.client.utils.RequestUtils.createSimpleHeaders;
import static com.icthh.xm.commons.config.domain.enums.ConfigEventType.UPDATE_CONFIG;

@Slf4j
@RequiredArgsConstructor
public class CommonConfigRepository {

    @Value("${application.tenant-with-creation-access-list:#{T(java.util.Set).of('XM')}}")
    private Set<String> allowedTenants;

    @Value("${spring.application.name}")
    private String applicationName;

    private static final String URL = "/api/private";
    private static final String VERSION = "version";

    private static final String TENANT_NAME = "tenantName";
    private static final String TENANT_PATH = "/config/tenants/{" + TENANT_NAME + "}/**";

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final RestTemplate restTemplate;
    private final XmConfigProperties xmConfigProperties;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final TenantContextHolder tenantContextHolder;

    private final ObjectMapper mapper = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .registerModule(new JavaTimeModule());

    public Map<String, Configuration> getConfig(String commit) {
        ParameterizedTypeReference<Map<String, Configuration>> typeRef = new ParameterizedTypeReference<Map<String, Configuration>>() {};
        HttpEntity<String> entity = new HttpEntity<>(createSimpleHeaders());
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(getServiceConfigUrl() + "/config_map").queryParam(VERSION, commit);
        return restTemplate.exchange(builder.toUriString(), HttpMethod.GET, entity, typeRef).getBody();
    }

    private String getServiceConfigUrl() {
        return xmConfigProperties.getXmConfigUrl() + URL;
    }

    public Map<String,Configuration> getConfig(String version, Collection<String> paths) {
        ParameterizedTypeReference<Map<String, Configuration>> typeRef = new ParameterizedTypeReference<Map<String, Configuration>>() {};
        HttpEntity<GetConfigRequest> entity = new HttpEntity<>(new GetConfigRequest(version, paths), createApplicationJsonHeaders());
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(getServiceConfigUrl() + "/config_map");
        return restTemplate.exchange(builder.toUriString(), HttpMethod.POST, entity, typeRef).getBody();
    }

    public void updateConfigFullPath(Configuration configuration, String oldConfigHash) {
        String tenantKey = getValidatedTenantKey(configuration.getPath());
        String topicName = String.format(xmConfigProperties.getKafkaConfigQueue(), tenantKey);
        ConfigurationUpdateMessage message = new ConfigurationUpdateMessage(configuration, oldConfigHash);

        log.info("Sending update configuration message event to kafka-topic = '{}', data = '{}'", topicName, message);
        kafkaTemplate.send(topicName, buildSystemEvent(message, tenantKey));
    }

    private String getValidatedTenantKey(String configurationPath) {
        String contextTenantKey = tenantContextHolder.getTenantKey();
        String pathTenantKey = pathMatcher.extractUriTemplateVariables(TENANT_PATH, configurationPath).get(TENANT_NAME);

        if (!StringUtils.equals(contextTenantKey, pathTenantKey) && !allowedTenants.contains(contextTenantKey)) {
            throw new BusinessException(String.format("Current configuration update is not allowed for tenant %s. " +
                "Check your current tenant or a list of tenants with creation access.", pathTenantKey));
        }
        return pathTenantKey;
    }

    private String buildSystemEvent(ConfigurationUpdateMessage message, String tenantKey) {
        return toJson(ConfigQueueEvent.builder()
            .eventId(MdcUtils.getRid())
            .messageSource(applicationName)
            .eventType(UPDATE_CONFIG.name())
            .tenantKey(tenantKey)
            .startDate(Instant.now())
            .data(message)
            .build());
    }

    @SneakyThrows
    private String toJson(ConfigQueueEvent event) {
        return mapper.writeValueAsString(event);
    }

}
