package com.icthh.xm.commons.lep.groovy;

import com.icthh.xm.commons.config.client.service.TenantAliasService;
import com.icthh.xm.commons.lep.api.LepEngine;
import com.icthh.xm.commons.lep.api.LepEngineFactory;
import com.icthh.xm.commons.lep.api.XmLepConfigFile;
import com.icthh.xm.commons.lep.impl.utils.ClassPathLepRepository;
import groovy.util.logging.Slf4j;
import org.springframework.beans.factory.BeanClassLoaderAware;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class GroovyLepEngineFactory extends LepEngineFactory implements BeanClassLoaderAware {

    public static final String GROOVY_SUFFIX = ".groovy";
    private final String appName;
    private final Map<String, XmLepConfigFile> defaultLeps;
    private final TenantAliasService tenantAliasService;
    private volatile ClassLoader classLoader;

    public GroovyLepEngineFactory(String appName,
                                  ClassPathLepRepository classPathLepRepository,
                                  TenantAliasService tenantAliasService) {
        super(appName);
        this.appName = appName;
        this.defaultLeps = classPathLepRepository.getLepFilesFromResources();
        this.tenantAliasService = tenantAliasService;
    }

    @Override
    public LepEngine createLepEngine(String tenant, List<XmLepConfigFile> configInLepFolder) {
        Map<String, XmLepConfigFile> classPathDefaultLeps = new HashMap<>();
        this.defaultLeps.forEach((key, value) -> {
            String path = tenant + "/" + appName + "/lep" + value.getPath();
            classPathDefaultLeps.put(path, new XmLepConfigFile(path, value.getContent()));
        });

        String configPrefix = "/config/tenants/";
        Map<String, XmLepConfigFile> lepsFromConfig = new HashMap<>();
        configInLepFolder.forEach((value) -> {
            String path = value.getPath().substring(configPrefix.length());
            lepsFromConfig.put(path, new XmLepConfigFile(path, value.getContent()));
        });

        Map<String, XmLepConfigFile> lepFiles = new HashMap<>();
        lepFiles.putAll(classPathDefaultLeps);
        lepFiles.putAll(lepsFromConfig);

        Map<String, XmLepConfigFile> leps = new HashMap<>();
        lepFiles.forEach((key, lep) -> {
            if (key.endsWith(GROOVY_SUFFIX)) {
                key = key.substring(0, key.length() - GROOVY_SUFFIX.length());
                leps.put(key, new XmLepConfigFile(key, lep.getContent()));
            }
        });

        return new GroovyLepEngine(appName, tenant, leps, tenantAliasService, classLoader);
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }
}
