package com.icthh.xm.commons.lep;

import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.lep.api.LepManagementService;
import com.icthh.xm.commons.lep.api.XmLepConfigFile;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.AntPathMatcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

@Slf4j
public class XmLepScriptConfigServerResourceLoader implements RefreshableConfiguration {

    private static final String commonsLepScriptsAntPathPattern = "/config/tenants/{tenantKey}/commons/lep/**";
    private static final String environmentLepScriptsAntPathPattern = "/config/tenants/commons/lep/**";
    private static final String TENANT_NAME = "tenantKey";
    private static final String ENV_COMMONS = "commons";

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final Map<String, Map<String, XmLepConfigFile>> scriptsByTenant = new ConcurrentHashMap<>();

    private final String tenantLepScriptsAntPathPattern;
    private final LepManagementService lepManagementService;
    private final RefreshTaskExecutor refreshExecutor;

    public XmLepScriptConfigServerResourceLoader(@Value("${spring.application.name}") String appName,
                                                 LepManagementService lepManagementService,
                                                 RefreshTaskExecutor refreshExecutor) {
        Objects.requireNonNull(appName, "appName can't be null");
        this.tenantLepScriptsAntPathPattern = "/config/tenants/{tenantKey}/" + appName + "/lep/**";
        this.lepManagementService = lepManagementService;
        this.refreshExecutor = refreshExecutor;
    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        return pathMatcher.match(tenantLepScriptsAntPathPattern, updatedKey)
            || pathMatcher.match(commonsLepScriptsAntPathPattern, updatedKey)
            || pathMatcher.match(environmentLepScriptsAntPathPattern, updatedKey);
    }

    @Override
    public void onRefresh(String updatedKey, String configContent) {
        String tenant = getTenant(updatedKey);
        scriptsByTenant.computeIfAbsent(tenant, (path) -> new ConcurrentHashMap<>());

        if (StringUtils.isBlank(configContent)) {
            scriptsByTenant.get(tenant).remove(updatedKey);
        } else {
            XmLepConfigFile xmLepConfigFile = new XmLepConfigFile(updatedKey, configContent);
            scriptsByTenant.get(tenant).put(updatedKey, xmLepConfigFile);
        }
    }

    @Override
    public void refreshFinished(Collection<String> paths) {
        Collection<XmLepConfigFile> envConfigs = getConfigByTenant(ENV_COMMONS);

        Set<String> tenantsToUpdate = getTenantsToUpdate(paths);

        Map<String, List<XmLepConfigFile>> configToUpdate = new HashMap<>();
        tenantsToUpdate.forEach(tenant -> {
            List<XmLepConfigFile> tenantConfigToUpdate = new ArrayList<>();
            tenantConfigToUpdate.addAll(envConfigs);
            tenantConfigToUpdate.addAll(getConfigByTenant(tenant));
            // very important that we copy call collections before pass to thread
            configToUpdate.put(tenant, tenantConfigToUpdate);
        });

        refreshExecutor.submit(() -> {
            try {
                lepManagementService.refreshEngines(configToUpdate);
            } catch (Throwable e) {
                log.error("Error during refresh configs {}", paths, e);
            }
        });
    }

    private Collection<XmLepConfigFile> getConfigByTenant(String tenant) {
        Map<String, XmLepConfigFile> configMap = scriptsByTenant.get(tenant);
        return configMap == null ? List.of() : configMap.values();
    }

    private Set<String> getTenantsToUpdate(Collection<String> paths) {
        TenantsByPathResponse tenantsByPaths = getTenantsByPaths(paths);
        if (tenantsByPaths.hasEnvCommons) {
            return scriptsByTenant.keySet();
        } else {
            return tenantsByPaths.getTenants();
        }
    }

    public TenantsByPathResponse getTenantsByPaths(Collection<String> paths) {
        List<String> tenantsInPath = paths.stream().map(this::getTenant).collect(toList());
        boolean hasEnvCommons = tenantsInPath.stream().anyMatch(ENV_COMMONS::equals);
        Set<String> tenants = tenantsInPath.stream().filter(not(ENV_COMMONS::equals)).collect(toSet());
        return new TenantsByPathResponse(tenants, hasEnvCommons);
    }

    private String getTenant(String path) {
        if (pathMatcher.match(tenantLepScriptsAntPathPattern, path)) {
            return pathMatcher.extractUriTemplateVariables(tenantLepScriptsAntPathPattern, path).get(TENANT_NAME);
        } else if (pathMatcher.match(commonsLepScriptsAntPathPattern, path)) {
            return pathMatcher.extractUriTemplateVariables(commonsLepScriptsAntPathPattern, path).get(TENANT_NAME);
        } else {
            return ENV_COMMONS;
        }
    }

    @Getter
    @RequiredArgsConstructor
    public static class TenantsByPathResponse {
        private final Set<String> tenants;
        private final boolean hasEnvCommons;
    }

}
