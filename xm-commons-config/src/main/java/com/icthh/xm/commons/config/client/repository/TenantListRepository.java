package com.icthh.xm.commons.config.client.repository;

import static com.fasterxml.jackson.databind.type.TypeFactory.defaultInstance;
import static com.icthh.xm.commons.config.client.utils.RequestUtils.createAuthHeaders;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableSet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.MapType;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.config.client.config.XmConfigProperties;
import com.icthh.xm.commons.config.domain.Configuration;
import com.icthh.xm.commons.config.domain.TenantState;
import java.util.Collections;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class TenantListRepository implements RefreshableConfiguration {

    public static final String TENANTS_LIST_CONFIG_KEY = "/config/tenants/tenants-list.json";

    public static final String URL = "/api/tenants/";

    private static final String ERROR = "Tenant list not found. Maybe xm-config not running.";

    private static final String SUSPENDED_STATE = "SUSPENDED";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final RestTemplate restTemplate;

    private final String applicationName;

    private final String xmConfigUrl;

    private volatile Set<TenantState> tenants = new HashSet<>();
    private volatile Set<String> suspendedTenants = new HashSet<>();

    public TenantListRepository(RestTemplate restTemplate,
                                CommonConfigRepository commonConfigRepository,
                                String applicationName,
                                XmConfigProperties applicationProperties) {
        this.restTemplate = restTemplate;
        this.applicationName = applicationName;
        this.xmConfigUrl = applicationProperties.getXmConfigUrl() + URL;

        Configuration configuration = commonConfigRepository.getConfig(null, singletonList(TENANTS_LIST_CONFIG_KEY)).
            get(TENANTS_LIST_CONFIG_KEY);
        if (configuration == null) {
            log.error(ERROR);
            throw new IllegalStateException(ERROR);
        }
        onInit(TENANTS_LIST_CONFIG_KEY, configuration.getContent());
    }

    public Set<String> getTenants() {
        if (CollectionUtils.isEmpty(tenants)) {
            log.error(ERROR);
            throw new IllegalStateException(ERROR);
        }
        return unmodifiableSet(tenants.stream().map(TenantState::getName).collect(Collectors.toSet()));
    }

    public Set<String> getSuspendedTenants() {
        return unmodifiableSet(suspendedTenants);
    }

    public void addTenant(String tenantName) {
        HttpEntity<String> entity = new HttpEntity<>(tenantName.toLowerCase(), createAuthHeaders());
        restTemplate.postForEntity(xmConfigUrl + applicationName, entity, Void.class);
    }

    public void deleteTenant(String tenantName) {
        HttpEntity<String> entity = new HttpEntity<>(createAuthHeaders());
        restTemplate.exchange(xmConfigUrl + applicationName + "/" + tenantName.toLowerCase(),
                              HttpMethod.DELETE, entity, Void.class);
    }

    public void updateTenant(String tenantName, String state) {
        HttpEntity<String> entity = new HttpEntity<>(state, createAuthHeaders());
        restTemplate.exchange(xmConfigUrl + applicationName + "/" + tenantName.toLowerCase(),
                              HttpMethod.PUT, entity, Void.class);
    }

    @SneakyThrows
    private void updateTenants(String key, String config) {
        log.info("Tenants list was updated");

        if (!TENANTS_LIST_CONFIG_KEY.equals(key)) {
            throw new IllegalArgumentException("Wrong config key to update " + key);
        }

        assertExistsTenantsListConfig(config);

        CollectionType setType = defaultInstance().constructCollectionType(HashSet.class, TenantState.class);
        MapType type = defaultInstance().constructMapType(HashMap.class, defaultInstance().constructType(String.class), setType);
        Map<String, Set<TenantState>> tenantsByServiceMap = objectMapper.readValue(config, type);
        Set<TenantState> tenantKeys = tenantsByServiceMap.get(applicationName);

        assertExistTenants(tenantKeys);

        this.tenants = tenantKeys;
        this.suspendedTenants = tenantKeys.stream().filter(tenant -> SUSPENDED_STATE.equals(tenant.getState()))
            .map(TenantState::getName).collect(Collectors.toSet());
    }

    private void assertExistTenants(Set<TenantState> tenantKeys) {
        if (CollectionUtils.isEmpty(tenantKeys)) {
            final String error = "Tenant list for " + applicationName + " empty. Check tenants-list.json.";
            log.error(error);
            throw new IllegalStateException(error);
        }
    }

    private void assertExistsTenantsListConfig(String config) {
        if (StringUtils.isBlank(config)) {
            log.error(ERROR);
            throw new IllegalStateException(ERROR);
        }
    }

    @Override
    public void onRefresh(String key, String config) {
        updateTenants(key, config);
    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        return TENANTS_LIST_CONFIG_KEY.equals(updatedKey);
    }

    @Override
    public void onInit(String key, String config) {
        updateTenants(key, config);
    }
}
