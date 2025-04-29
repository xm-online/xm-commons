package com.icthh.xm.commons.permission.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.lep.api.LepAdditionalContext;
import com.icthh.xm.commons.lep.api.LepAdditionalContextField;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.commons.permission.domain.dto.PermissionContextDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Map;
import java.util.TreeMap;

import static com.icthh.xm.commons.config.client.config.XmRestTemplateConfiguration.XM_CONFIG_REST_TEMPLATE;
import static com.icthh.xm.commons.config.client.utils.RequestUtils.createAuthHeaders;

@Slf4j
@Service
public class PermissionContextCheckService implements LepAdditionalContext<PermissionContextCheckService> {

    private static final String API = "/api";

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${application.permission-context-uri:/uaa/api/account}")
    private String permissionContextUri;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public PermissionContextCheckService(@Qualifier(XM_CONFIG_REST_TEMPLATE) RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper()
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .registerModule(new JavaTimeModule());
    }

    public boolean hasPermission(String permission) {
        return hasPermission(permission, Map.of());
    }

    public boolean hasPermission(String permission, Map<String, Object> expectedContextData) {
        PermissionContextDto contextDto = getPermissionContext();

        boolean isValidPermission = contextDto.getPermissions().contains(permission);
        boolean isValidContextDate = expectedContextData.entrySet().stream()
            .allMatch(e -> {
                Map<String, Object> actual = contextDto.getCtx();
                return actual.containsKey(e.getKey()) && actual.get(e.getKey()).equals(e.getValue());
            });
        return isValidPermission && isValidContextDate;
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

    @Override
    @IgnoreLogginAspect
    public String additionalContextKey() {
        return PermissionContextCheckServiceField.FIELD_NAME;
    }

    @Override
    @IgnoreLogginAspect
    public PermissionContextCheckService additionalContextValue() {
        return this;
    }

    @Override
    @IgnoreLogginAspect
    public Class<? extends LepAdditionalContextField> fieldAccessorInterface() {
        return PermissionContextCheckServiceField.class;
    }

    public interface PermissionContextCheckServiceField extends LepAdditionalContextField {
        String FIELD_NAME = "permissionContextCheckService";
        default PermissionContextCheckService getPermissionContextCheckService() {
            return (PermissionContextCheckService)get(FIELD_NAME);
        }
    }
}
