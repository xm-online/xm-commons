package com.icthh.xm.commons.lep.spring;

import com.icthh.xm.commons.lep.RefreshTaskExecutor;
import com.icthh.xm.commons.lep.api.LepManagementService;
import com.icthh.xm.commons.lep.api.XmLepConfigFile;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.stereotype.Service;

import static com.icthh.xm.commons.lep.utils.XmLepUtils.prepareConfigs;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Slf4j
@Service
@RequiredArgsConstructor
@IgnoreLogginAspect
public class LepRefreshServiceImpl implements LepRefreshService {

    private final LepManagementService lepManagementService;
    private final RefreshTaskExecutor refreshExecutor = new RefreshTaskExecutor();

    @Override
    public Future<Boolean> refreshEngines(Set<String> tenantsToUpdate, Map<String, Map<String, XmLepConfigFile>> scriptsByTenant, boolean isInit) {
        return refreshEngines(tenantsToUpdate, scriptsByTenant, isInit, null);
    }

    @Override
    @SneakyThrows
    public void initOrRefresh(Set<String> tenantsToUpdate, Map<String, Map<String, XmLepConfigFile>> scriptsByTenant, String pathToPrecompiledLep) {
        if (!lepManagementService.isLepEnginesInited()) {
            initEngines(tenantsToUpdate, scriptsByTenant, pathToPrecompiledLep);
        } else {
            refreshEngines(tenantsToUpdate, scriptsByTenant, false, pathToPrecompiledLep).get();
        }
    }

    private Future<Boolean> refreshEngines(Set<String> tenantsToUpdate,
                                    Map<String, Map<String, XmLepConfigFile>> scriptsByTenant,
                                    boolean isInit,
                                    String pathToWorkingDirectory) {
        log.info("Submit task for update lep engines for tenants {}", tenantsToUpdate);
        return refreshExecutor.submit(() -> {
            try {
                if (isInit && lepManagementService.isLepEnginesInited()) {
                    return false;
                }

                Map<String, List<XmLepConfigFile>> configToUpdate = prepareConfigs(tenantsToUpdate, scriptsByTenant);
                if (StringUtils.isNotBlank(pathToWorkingDirectory)) {
                    lepManagementService.refreshEngines(configToUpdate, pathToWorkingDirectory);
                } else {
                    lepManagementService.refreshEngines(configToUpdate);
                }

                return true;
            } catch (Throwable e) {
                log.error("Error during refresh configs: {}", e.getMessage(), e);
                return false;
            }
        });
    }

    @SneakyThrows
    private void initEngines(Set<String> tenantsToUpdate, Map<String, Map<String, XmLepConfigFile>> scriptsByTenant, String pathToPrecompiledLep) {
        StopWatch stopWatch = StopWatch.createStarted();
        log.info("START | Init lep engines for tenants {}", tenantsToUpdate);
        refreshEngines(tenantsToUpdate, scriptsByTenant, true, pathToPrecompiledLep).get();
        log.info("STOP | Leps inited, time: {}ms", stopWatch.getTime(MILLISECONDS));
    }
}
