package com.icthh.xm.commons.lep.groovy.storage;

import com.icthh.xm.commons.config.client.service.TenantAliasService;
import com.icthh.xm.commons.lep.api.XmLepConfigFile;
import com.icthh.xm.commons.lep.impl.utils.ClassPathLepRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.reverse;

public class ClassPathLepStorageFactory implements LepStorageFactory {

    private final String appName;
    private final Map<String, XmLepConfigFile> defaultLeps;
    private final Map<String, XmLepConfigFile> classPathLeps;
    private final TenantAliasService tenantAliasService;

    public ClassPathLepStorageFactory(String appName,
                                      ClassPathLepRepository classPathLepRepository,
                                      TenantAliasService tenantAliasService) {
        this.appName = appName;
        this.defaultLeps = classPathLepRepository.getLepFilesFromResources("lep/default");
        this.classPathLeps = classPathLepRepository.getLepFilesFromResources("lep/custom");
        this.tenantAliasService = tenantAliasService;
    }

    @Override
    public XmConfigLepStorage buildXmConfigLepStorage(String tenant, List<XmLepConfigFile> lepFromConfig) {
        Map<String, XmLepConfigFile> classPathDefaultLeps = new HashMap<>();
        this.defaultLeps.forEach((key, value) -> {
            String path = tenant + "/" + appName + "/lep" + value.getPath();
            classPathDefaultLeps.put(path, new XmLepConfigFile(path, value.getContent()));
        });

        List<String> parentTenants = tenantAliasService.getTenantAliasTree().getParentKeys(tenant);
        parentTenants.add(0, tenant.toLowerCase());
        reverse(parentTenants);

        Map<String, XmLepConfigFile> lepsFromClassPath = new HashMap<>();
        parentTenants.forEach(tenantKey -> {
            classPathLeps.values().forEach((value) -> {
                String path = value.getPath();
                String pathTenantKey = path.substring(0, path.indexOf("/"));
                if (pathTenantKey.equalsIgnoreCase(tenantKey)) {
                    path = tenant.toUpperCase() + "/" + path.substring(path.indexOf("/"));
                    lepsFromClassPath.put(path, new XmLepConfigFile(path, value.getContent()));
                }
            });
        });

        Map<String, XmLepConfigFile> lepFiles = new HashMap<>();
        lepFiles.putAll(classPathDefaultLeps);
        lepFiles.putAll(lepsFromClassPath);

        Map<String, XmLepConfigFile> leps = new HashMap<>();
        lepFiles.forEach((key, lep) -> {
            if (key.endsWith(GROOVY_SUFFIX)) {
                key = key.substring(0, key.length() - GROOVY_SUFFIX.length());
                leps.put(key, new XmLepConfigFile(key, lep.getContent()));
            }
        });

        return new XmConfigLepStorage(leps);
    }

}
