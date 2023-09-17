package com.icthh.xm.commons.lep;

import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.lep.api.LepExecutorResolver;
import com.icthh.xm.commons.lep.api.LepManagementService;
import com.icthh.xm.commons.lep.api.XmLepConfigFile;
import com.icthh.xm.commons.lep.spring.ApplicationNameProvider;
import com.icthh.xm.commons.lep.spring.LepUpdateMode;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.util.AntPathMatcher;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static com.icthh.xm.commons.lep.spring.LepUpdateMode.SYNCHRONOUS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

@Slf4j
public class XmLepScriptConfigServerResourceLoader implements RefreshableConfiguration, SmartInitializingSingleton {

    private static final String commonsLepScriptsAntPathPattern = "/config/tenants/{tenantKey}/commons/lep/**";
    private static final String environmentLepScriptsAntPathPattern = "/config/tenants/commons/lep/**";
    private static final String TENANT_NAME = "tenantKey";
    private static final String ENV_COMMONS = "commons";

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final Map<String, Map<String, XmLepConfigFile>> scriptsByTenant = new ConcurrentHashMap<>();
    private final RefreshTaskExecutor refreshExecutor = new RefreshTaskExecutor();

    private final String tenantLepScriptsAntPathPattern;
    private final LepManagementService lepManagementService;
    private final LepUpdateMode lepUpdateMode;
    private final TenantContextHolder tenantContextHolder;

    public XmLepScriptConfigServerResourceLoader(ApplicationNameProvider applicationNameProvider,
                                                 LepManagementService lepManagementService,
                                                 LepUpdateMode lepUpdateMode,
                                                 TenantContextHolder tenantContextHolder) {
        String appName = applicationNameProvider.getAppName();
        this.tenantLepScriptsAntPathPattern = "/config/tenants/{tenantKey}/" + appName + "/lep/**";
        this.lepManagementService = lepManagementService;
        this.lepUpdateMode = lepUpdateMode;
        this.tenantContextHolder = tenantContextHolder;
    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        return pathMatcher.match(tenantLepScriptsAntPathPattern, updatedKey)
            || pathMatcher.match(commonsLepScriptsAntPathPattern, updatedKey)
            || pathMatcher.match(environmentLepScriptsAntPathPattern, updatedKey);
    }

    @Override
    @SneakyThrows
    public void onRefresh(String updatedKey, String configContent) {
        String tenant = getTenant(updatedKey);
        scriptsByTenant.computeIfAbsent(tenant, (path) -> new ConcurrentHashMap<>());

        if (StringUtils.isBlank(configContent)) {
            scriptsByTenant.get(tenant).remove(updatedKey);
        } else {
            XmLepConfigFile xmLepConfigFile = new XmLepConfigFile(updatedKey, configContent);
            scriptsByTenant.get(tenant).put(updatedKey, xmLepConfigFile);
        }

        refreshImmediatelyIfSynchronousMode(updatedKey);
    }

    private void refreshImmediatelyIfSynchronousMode(String updatedKey) throws InterruptedException, ExecutionException {
        if (SYNCHRONOUS.equals(lepUpdateMode)) {
            Set<String> tenantsToUpdate = getTenantsToUpdate(List.of(updatedKey));
            refreshEngines(tenantsToUpdate, false).get();
            // if refresh operation invoked in thread where inited threadLepContext, threadLepContext have to be reinited
            LepExecutorResolver currentLepExecutorResolver = lepManagementService.getCurrentLepExecutorResolver();
            if (currentLepExecutorResolver != null && tenantContextHolder.getContext().isInitialized()) {
                lepManagementService.endThreadContext();
                lepManagementService.beginThreadContext();
            }
        }
    }

    @Override
    @SneakyThrows
    public void refreshFinished(Collection<String> paths) {
        Set<String> tenantsToUpdate = getTenantsToUpdate(paths);
        Future<?> future = refreshEngines(tenantsToUpdate, false);
        if (SYNCHRONOUS.equals(lepUpdateMode)) {
            future.get();
        }
    }

    private Future<?> refreshEngines(Set<String> tenantsToUpdate, boolean isInit) {
        log.info("Submit task for update lep engines for tenants {}", tenantsToUpdate);
        return refreshExecutor.submit(() -> {
            try {
                // if not init - it`s refresh, if it`s init we need to check that not inited yet
                if (isInit && lepManagementService.isLepEnginesInited()) {
                    return false;
                }

                Map<String, List<XmLepConfigFile>> configToUpdate = prepareConfigs(tenantsToUpdate);
                lepManagementService.refreshEngines(configToUpdate);
                return true;
            } catch (Throwable e) {
                log.error("Error during refresh configs", e);
                return false;
            }
        });
    }

    private Map<String, List<XmLepConfigFile>> prepareConfigs(Set<String> tenantsToUpdate) {
        Collection<XmLepConfigFile> envConfigs = getConfigByTenant(ENV_COMMONS);
        Map<String, List<XmLepConfigFile>> configToUpdate = new HashMap<>();
        tenantsToUpdate.forEach(tenant -> {
            List<XmLepConfigFile> tenantConfigToUpdate = new ArrayList<>();
            tenantConfigToUpdate.addAll(envConfigs);
            tenantConfigToUpdate.addAll(getConfigByTenant(tenant));
            // very important that we copy call collections before pass to thread
            configToUpdate.put(tenant, tenantConfigToUpdate);
        });
        return configToUpdate;
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

    @SneakyThrows
    @PostConstruct // unit test don't throw ApplicationReadyEvent
    public void init() {
        StopWatch stopWatch = StopWatch.createStarted();
        // in case when no lep exists we need to init lep engines to pass await
        Set<String> tenantsToUpdate = scriptsByTenant.keySet();
        log.info("START | Start init leps for tenants {}", tenantsToUpdate);
        refreshEngines(tenantsToUpdate, true).get(); // wait before lep will be inited
        log.info("STOP | Leps inited, time: {}ms", stopWatch.getTime(MILLISECONDS));
    }

    @Override
    public void afterSingletonsInstantiated() {
        init();
    }

    @Getter
    @RequiredArgsConstructor
    public static class TenantsByPathResponse {
        private final Set<String> tenants;
        private final boolean hasEnvCommons;
    }

}
