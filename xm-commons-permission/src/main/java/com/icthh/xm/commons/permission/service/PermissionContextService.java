package com.icthh.xm.commons.permission.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.permission.domain.dto.PermissionContextDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Map;
import java.util.TreeMap;

import static com.icthh.xm.commons.config.client.config.XmRestTemplateConfiguration.XM_CONFIG_REST_TEMPLATE;
import static com.icthh.xm.commons.config.client.utils.RequestUtils.createAuthHeaders;

@Slf4j
@LepService(group = "service.permission")
public class PermissionContextService {

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${application.permission-context-uri:'/uaa/api/account'}")
    private String permissionContextUri;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public PermissionContextService(@Qualifier(XM_CONFIG_REST_TEMPLATE) RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper()
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .registerModule(new JavaTimeModule());
    }

    @LogicExtensionPoint("Context")
    public PermissionContextDto getPermissionContext(String userKey) {
        log.info("Get empty auth permission context by userKey: {}", userKey);
        return new PermissionContextDto();
    }

    public boolean hasPermission(String permission) {
        return hasPermission(permission, Map.of());
    }

    public boolean hasPermission(String permission, Map<String, Object> expectedContextData) {
        PermissionContextDto contextDto = getPermissionContext();

        boolean permissionExists = contextDto.getPermissions().contains(permission);
        boolean contextKeyExists = expectedContextData.entrySet().stream()
            .allMatch(e -> {
                Map<String, Object> actual = contextDto.getCtx();
                return actual.containsKey(e.getKey()) && actual.get(e.getKey()).equals(e.getValue());
            });
        return permissionExists && contextKeyExists;
    }

    private PermissionContextDto getPermissionContext() {
        log.info("Try to get permission context");
        try {
            HttpEntity<Void> request = new HttpEntity<>(createAuthHeaders());
            URI uri = new URI("http://" + permissionContextUri);
            Map result = restTemplate.exchange(uri, HttpMethod.GET, request, Map.class).getBody();
            log.debug("Obtained permission context: {}", result);

            Map<String, PermissionContextDto> serviceContextMapping = writeContextAsMap(result.get("context"));
            return serviceContextMapping.get(applicationName);

        } catch (Exception ex) {
            log.error("Failed to fetch permission context from app {}: {}", applicationName, ex.getMessage());
            throw new BusinessException("error.permission.context.fetch", ex.getMessage());
        }
    }

    private Map<String, PermissionContextDto> writeContextAsMap(Object context) {
        return objectMapper.convertValue(context, new TypeReference<TreeMap<String, PermissionContextDto>>() {});
    }
}
