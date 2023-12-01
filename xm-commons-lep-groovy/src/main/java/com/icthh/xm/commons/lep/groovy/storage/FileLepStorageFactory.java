package com.icthh.xm.commons.lep.groovy.storage;

import com.icthh.xm.commons.config.client.service.TenantAliasService;
import com.icthh.xm.commons.lep.LepPathResolver;
import com.icthh.xm.commons.lep.api.XmLepConfigFile;
import com.icthh.xm.commons.lep.impl.utils.ClassPathLepRepository;

import java.util.List;
import java.util.Map;

public class FileLepStorageFactory implements LepStorageFactory {

    private final String appName;
    private final Map<String, XmLepConfigFile> defaultLeps;
    private final TenantAliasService tenantAliasService;
    private final LepPathResolver lepPathResolver;
    private final String baseDir;

    public FileLepStorageFactory(String appName,
                                 ClassPathLepRepository classPathLepRepository,
                                 TenantAliasService tenantAliasService,
                                 LepPathResolver lepPathResolver,
                                 String baseDir) {
        this.appName = appName;
        this.defaultLeps = classPathLepRepository.getLepFilesFromResources("lep/default");
        this.tenantAliasService = tenantAliasService;
        this.lepPathResolver = lepPathResolver;
        this.baseDir = baseDir;
    }

    @Override
    public LepStorage buildXmConfigLepStorage(String tenant, List<XmLepConfigFile> lepFromConfig) {
        return new FileLepStorage(tenant, appName, defaultLeps, tenantAliasService, lepPathResolver, baseDir);
    }

}
