package com.icthh.xm.commons.lep.utils;

import com.icthh.xm.commons.lep.api.XmLepConfigFile;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

import static com.icthh.xm.commons.lep.LepPathResolver.ENV_COMMONS;

@UtilityClass
public class XmLepUtils {

    public static void addToScriptsByTenant(String tenant,
                                            Map<String, Map<String, XmLepConfigFile>> scriptsByTenant,
                                            String path,
                                            String content) {
        scriptsByTenant.computeIfAbsent(tenant, k -> new HashMap<>());

        if (StringUtils.isBlank(content)) {
            scriptsByTenant.get(tenant).remove(path);
        } else {
            scriptsByTenant.get(tenant).put(path, new XmLepConfigFile(path, content));
        }
    }

    public static Map<String, List<XmLepConfigFile>> prepareConfigs(Set<String> tenantsToUpdate,
                                                                    Map<String, ? extends Map<String, XmLepConfigFile>> scriptsByTenant) {
        Collection<XmLepConfigFile> envConfigs = getConfigByTenant(ENV_COMMONS, scriptsByTenant);
        Map<String, List<XmLepConfigFile>> configToUpdate = new HashMap<>();
        tenantsToUpdate.forEach(tenant -> {
            List<XmLepConfigFile> tenantConfigToUpdate = new ArrayList<>();
            tenantConfigToUpdate.addAll(envConfigs);
            tenantConfigToUpdate.addAll(getConfigByTenant(tenant, scriptsByTenant));
            // very important that we copy call collections before pass to thread
            configToUpdate.put(tenant, tenantConfigToUpdate);
        });
        return configToUpdate;
    }

    private static Collection<XmLepConfigFile> getConfigByTenant(String tenant,
                                                                 Map<String, ? extends Map<String, XmLepConfigFile>> scriptsByTenant) {

        Map<String, XmLepConfigFile> configMap = scriptsByTenant.get(tenant);
        return configMap == null ? List.of() : configMap.values();
    }
}
