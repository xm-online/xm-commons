package com.icthh.xm.commons.repository;

import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.icthh.xm.commons.domain.idp.IdpConstants.PUBLIC_JWKS_CONFIG_PATTERN;
import static com.icthh.xm.commons.domain.idp.IdpConstants.IDP_CLIENT_KEY;

/**
 * This class caches JWKS keys.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwksRepository implements RefreshableConfiguration {

    private static final String KEY_TENANT = "tenant";

    /**
     * This map stores jwks keys.
     * <p/>
     * This information is needed for token signature verification.
     * Each tenant has own idp clients list and each client has it's own JWKS keys.
     * Generally speaking, JWKS keys might be the same for list of clients, but there are no such guaranties.
     * For the sake of simplicity each client JWKS key stored separately.
     * <p/>
     * jwksStorage structure Map< tenantKey, Map< clientKey, JWKS > > where:
     * <ul>
     *    <li/> tenantKey - tenant id
     *    <li/> clientKey - client id
     *    <li/> JWKS - JWKS value
     * </ul>
     */
    private final Map<String, Map<String, String>> jwksStorage = new ConcurrentHashMap<>();

    private final TenantContextHolder tenantContextHolder;

    private final AntPathMatcher matcher = new AntPathMatcher();

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        return matcher.match(PUBLIC_JWKS_CONFIG_PATTERN, updatedKey);
    }

    @Override
    public void onInit(String configKey, String configValue) {
        processJwks(configKey, configValue);
    }

    @Override
    public void onRefresh(String updatedKey, String config) {
        processJwks(updatedKey, config);
    }

    private void processJwks(String configPath, String config) {
        String tenantKey = extractKeyFromPath(configPath, KEY_TENANT);
        String clientKey = extractKeyFromPath(configPath, IDP_CLIENT_KEY);

        saveJwks(config, tenantKey, clientKey);
    }

    private void saveJwks(String config, String tenantKey, String clientKey) {
        jwksStorage
            .computeIfAbsent(tenantKey, key -> new ConcurrentHashMap<>())
            .put(clientKey, config);

    }

    private String extractKeyFromPath(String configPath, String keyName) {
        return matcher
            .extractUriTemplateVariables(PUBLIC_JWKS_CONFIG_PATTERN, configPath)
            .get(keyName);
    }

    public Map<String, String> getTenantIdpJwks() {
        String tenantKey = tenantContextHolder.getTenantKey();

        return jwksStorage.getOrDefault(tenantKey, new HashMap<>());
    }
}
