package com.icthh.xm.commons.repository;

import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.icthh.xm.commons.domain.idp.IdpConstants.JWKS_FILE_NAME_PATTERN;
import static com.icthh.xm.commons.domain.idp.IdpConstants.PUBLIC_JWKS_CONFIG_PATH_PATTERN;
import static com.icthh.xm.commons.domain.idp.IdpConstants.IDP_CLIENT_KEY;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwksRepository implements RefreshableConfiguration {

    private static final String KEY_TENANT = "tenant";

    // FIXME: describe the meaning of Map keys
    private final Map<String, Map<String, String>> jwksStorage = new ConcurrentHashMap<>();

    private final AntPathMatcher matcher = new AntPathMatcher();

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        //FIXME: suggest intorduce one static variable: PUBLIC_JWKS_CONFIG_PATTERN = PUBLIC_JWKS_CONFIG_PATH_PATTERN + JWKS_FILE_NAME_PATTERN
        return matcher.match(PUBLIC_JWKS_CONFIG_PATH_PATTERN + JWKS_FILE_NAME_PATTERN, updatedKey);
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
        getIdpJwksByTenantKey(tenantKey); //FIXME: seems this invocation is useless here.
    }

    private void saveJwks(String config, String tenantKey, String clientKey) {
        Map<String, String> jwksRecord =
            jwksStorage.computeIfAbsent(tenantKey, key -> new ConcurrentHashMap<>());

        jwksRecord.put(clientKey, config);
        //FIXME: why do you use local variable (jwksRecord) and not just chain the calls? like that:
//        jwksStorage.computeIfAbsent(tenantKey, key -> new ConcurrentHashMap<>())
//                   .put(clientKey, config);

    }

    private String extractKeyFromPath(String configPath, String keyName) {
        //FIXME: the same question: why local variable?
        Map<String, String> configKeyParams =
            matcher.extractUriTemplateVariables(PUBLIC_JWKS_CONFIG_PATH_PATTERN + JWKS_FILE_NAME_PATTERN, configPath);

        return configKeyParams.get(keyName);
    }

    public Map<String, String> getIdpJwksByTenantKey(String tenantKey) {
        return jwksStorage.getOrDefault(tenantKey, new HashMap<>());
    }
}
