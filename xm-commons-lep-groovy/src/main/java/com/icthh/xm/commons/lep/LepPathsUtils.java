package com.icthh.xm.commons.lep;

import com.icthh.xm.commons.config.client.service.TenantAliasService;

import java.util.ArrayList;
import java.util.List;

public class LepPathsUtils {

    public static List<String> getLepBasePaths(String tenant, String appName, TenantAliasService tenantAliasService) {
        List<String> paths = new ArrayList<>(List.of(
            tenant + "/" + appName + "/lep",
            tenant + "/commons/lep",
            "commons/lep"
        ));
        List<String> tenantKeys = tenantAliasService.getTenantAliasTree().getParentKeys(tenant);
        tenantKeys.forEach(tenantKey -> {
            paths.add(tenantKey + "/" + appName + "/lep");
            paths.add(tenantKey + "/commons/lep");
        });
        return paths;
    }

}
