package com.icthh.xm.commons.config.client.repository;

import static com.fasterxml.jackson.databind.type.TypeFactory.defaultInstance;
import static com.icthh.xm.commons.config.client.utils.RequestUtils.createAuthHeaders;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableSet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.MapType;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.config.client.config.XmConfigProperties;
import com.icthh.xm.commons.config.domain.Configuration;
import com.icthh.xm.commons.config.domain.TenantState;
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
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
public class TenantListRepository implements RefreshableConfiguration {

    public static final String TENANTS_LIST_CONFIG_KEY = "/config/tenants/tenants-list.json";

    private static final String URL = "/api/tenants/";

    private static final String ERROR = "Tenant list not found. Maybe xm-config not running.";

    private static final String SUSPENDED_STATE = "SUSPENDED";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final RestTemplate restTemplate;

    private final String applicationName;

    private final String xmConfigUrl;

    private final Set<String> includeTenants;

    private volatile Set<TenantState> tenants = new HashSet<>();
    private volatile Set<String> suspendedTenants = new HashSet<>();

    public TenantListRepository(RestTemplate restTemplate,
                                CommonConfigRepository commonConfigRepository,
                                String applicationName,
                                XmConfigProperties xmConfigProperties) {
        this.restTemplate = restTemplate;
        this.applicationName = applicationName;
        this.xmConfigUrl = xmConfigProperties.getXmConfigUrl() + URL;
        this.includeTenants = xmConfigProperties.getIncludeTenantLowercase();

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

    @SuppressWarnings("unused")
    public void addTenant(String tenantName) {
        HttpEntity<String> entity = new HttpEntity<>(tenantName.toLowerCase(), createAuthHeaders());
        restTemplate.postForEntity(xmConfigUrl + applicationName, entity, Void.class);
    }

    @SuppressWarnings("unused")
    public void deleteTenant(String tenantName) {
        HttpEntity<String> entity = new HttpEntity<>(createAuthHeaders());
        restTemplate.exchange(xmConfigUrl + applicationName + "/" + tenantName.toLowerCase(),
                              HttpMethod.DELETE, entity, Void.class);
    }

    @SuppressWarnings("unused")
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

        Set<TenantState> tenantKeys = parseTenantStates(config, applicationName, objectMapper);

        assertExistTenants(tenantKeys);

        if (!includeTenants.isEmpty()) {
            log.warn("Tenant list was overridden by property 'xm-config.include-tenants' to: {}", includeTenants);
        }

        this.tenants = tenantKeys.stream()
                                 .filter(isIncluded(includeTenants))
                                 .collect(Collectors.toSet());

        this.suspendedTenants = tenantKeys.stream()
                                          .filter(isSuspended())
                                          .map(TenantState::getName)
                                          .collect(Collectors.toSet());
    }

    public static Predicate<TenantState> isIncluded(Set<String> includedTenants) {
        return tenantState -> includedTenants == null
                              || includedTenants.isEmpty()
                              || includedTenants.contains(tenantState.getName());
    }

    public static Predicate<TenantState> isSuspended() {
        return tenantState -> SUSPENDED_STATE.equals(tenantState.getState());
    }

    private static Set<TenantState> parseTenantStates(String tenantListJson, String appName, ObjectMapper mapper) throws java.io.IOException {
        return parseTenantStates(tenantListJson, mapper).get(appName);
    }

    /**
     * Parsses tenant-list.json to map by application and set of @{@link TenantState}
     * @param tenantListJson source tenant list
     * @param mapper ObjectMapper
     * @return Map with application name as a key and set of tenant states as a value.
     */
    public static Map<String, Set<TenantState>> parseTenantStates(String tenantListJson, ObjectMapper mapper) throws java.io.IOException {
        CollectionType setType = defaultInstance().constructCollectionType(HashSet.class, TenantState.class);
        MapType type = defaultInstance().constructMapType(HashMap.class, defaultInstance().constructType(String.class), setType);
        Map<String, Set<TenantState>> map = mapper.readValue(tenantListJson, type);
        return Optional.ofNullable(map).orElse(new HashMap<>());
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
