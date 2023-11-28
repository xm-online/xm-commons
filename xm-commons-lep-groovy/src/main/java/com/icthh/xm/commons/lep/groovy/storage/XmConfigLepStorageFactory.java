package com.icthh.xm.commons.lep.groovy.storage;

import com.icthh.xm.commons.lep.api.XmLepConfigFile;
import com.icthh.xm.commons.lep.impl.utils.ClassPathLepRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XmConfigLepStorageFactory implements LepStorageFactory {

    private final String appName;
    private final Map<String, XmLepConfigFile> defaultLeps;

    public XmConfigLepStorageFactory(String appName,
                                     ClassPathLepRepository classPathLepRepository) {
        this.appName = appName;
        this.defaultLeps = classPathLepRepository.getLepFilesFromResources("lep/default");
    }

    public XmConfigLepStorage buildXmConfigLepStorage(String tenant, List<XmLepConfigFile> lepFromConfig) {
        Map<String, XmLepConfigFile> classPathDefaultLeps = new HashMap<>();
        this.defaultLeps.forEach((key, value) -> {
            String path = tenant + "/" + appName + "/lep" + value.getPath();
            classPathDefaultLeps.put(path, new XmLepConfigFile(path, value.getContentStream()));
        });

        String configPrefix = "/config/tenants/";
        Map<String, XmLepConfigFile> lepsFromConfig = new HashMap<>();
        lepFromConfig.forEach((value) -> {
            String path = value.getPath().substring(configPrefix.length());
            lepsFromConfig.put(path, new XmLepConfigFile(path, value.getContentStream()));
        });

        Map<String, XmLepConfigFile> lepFiles = new HashMap<>();
        lepFiles.putAll(classPathDefaultLeps);
        lepFiles.putAll(lepsFromConfig);

        Map<String, XmLepConfigFile> leps = new HashMap<>();
        lepFiles.forEach((key, lep) -> {
            if (key.endsWith(GROOVY_SUFFIX)) {
                key = key.substring(0, key.length() - GROOVY_SUFFIX.length());
                leps.put(key, new XmLepConfigFile(key, lep.getContentStream()));
            }
        });

        return new XmConfigLepStorage(leps);
    }

}
