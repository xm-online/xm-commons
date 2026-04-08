package com.icthh.xm.commons.lep;

import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.lep.api.LepExecutorResolver;
import com.icthh.xm.commons.lep.api.LepManagementService;
import com.icthh.xm.commons.lep.api.XmLepConfigFile;
import com.icthh.xm.commons.lep.spring.LepUpdateMode;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.util.AntPathMatcher;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static com.icthh.xm.commons.lep.LepPathResolver.ENV_COMMONS;
import static com.icthh.xm.commons.lep.spring.LepUpdateMode.SYNCHRONOUS;
import static com.icthh.xm.commons.lep.utils.XmLepUtils.addToScriptsByTenant;
import static com.icthh.xm.commons.lep.utils.XmLepUtils.prepareConfigs;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

@Slf4j
public class XmLepScriptConfigServerResourceLoader implements RefreshableConfiguration, SmartInitializingSingleton {

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final Map<String, Map<String, XmLepConfigFile>> scriptsByTenant = new ConcurrentHashMap<>();
    private final RefreshTaskExecutor refreshExecutor = new RefreshTaskExecutor();

    private final LepManagementService lepManagementService;
    private final LepUpdateMode lepUpdateMode;
    private final TenantContextHolder tenantContextHolder;
    private final LepPathResolver lepPathResolver;
    private final List<String> lepPathPatterns;

    public XmLepScriptConfigServerResourceLoader(LepPathResolver lepPathResolver,
                                                 LepManagementService lepManagementService,
                                                 LepUpdateMode lepUpdateMode,
                                                 TenantContextHolder tenantContextHolder) {
        this.lepPathPatterns = lepPathResolver.getLepPathPatterns();
        this.lepPathResolver = lepPathResolver;
        this.lepManagementService = lepManagementService;
        this.lepUpdateMode = lepUpdateMode;
        this.tenantContextHolder = tenantContextHolder;
    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        return lepPathPatterns.stream().anyMatch(it -> pathMatcher.match(it, updatedKey));
    }

    @Override
    @SneakyThrows
    public void onRefresh(String updatedKey, String configContent) {
        String tenant = lepPathResolver.getTenantFromPath(updatedKey);
        scriptsByTenant.computeIfAbsent(tenant, (path) -> new ConcurrentHashMap<>());

        addToScriptsByTenant(tenant, scriptsByTenant, updatedKey, configContent);

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

                Map<String, List<XmLepConfigFile>> configToUpdate = prepareConfigs(tenantsToUpdate, scriptsByTenant);
                lepManagementService.refreshEngines(configToUpdate);
                return true;
            } catch (Throwable e) {
                log.error("Error during refresh configs: {}", e.getMessage(), e);
                return false;
            }
        });
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
        List<String> tenantsInPath = paths.stream().map(lepPathResolver::getTenantFromPath).collect(toList());
        boolean hasEnvCommons = tenantsInPath.stream().anyMatch(ENV_COMMONS::equals);
        Set<String> tenants = tenantsInPath.stream().filter(not(ENV_COMMONS::equals)).collect(toSet());
        return new TenantsByPathResponse(tenants, hasEnvCommons);
    }

    @SneakyThrows
    private void init() {
        StopWatch stopWatch = StopWatch.createStarted();
        // in case when no lep exists we need to init lep engines to pass await
        Set<String> tenantsToUpdate = scriptsByTenant.keySet();
        log.info("START | Start init leps for tenants {}", tenantsToUpdate);
        refreshEngines(tenantsToUpdate, true).get(); // wait before lep will be inited
        log.info("STOP | Leps inited, time: {}ms", stopWatch.getTime(MILLISECONDS));
    }

    @Override
    public void refreshableConfigurationInited() {
        if (!lepManagementService.isLepEnginesInited()) {
            log.info("Init lep engine by refreshableConfigurationInited");
            init();
        }
    }

    @PostConstruct // for unit that don't init InitRefreshableConfigurationBeanPostProcessor
    public void afterSingletonsInstantiated() {
        if (lepUpdateMode == SYNCHRONOUS) {
            init();
        }
    }

    @Getter
    @RequiredArgsConstructor
    public static class TenantsByPathResponse {
        private final Set<String> tenants;
        private final boolean hasEnvCommons;
    }

}
