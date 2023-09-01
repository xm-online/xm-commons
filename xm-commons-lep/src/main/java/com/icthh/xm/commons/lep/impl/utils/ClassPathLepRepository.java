package com.icthh.xm.commons.lep.impl.utils;

import com.icthh.xm.commons.lep.api.XmLepConfigFile;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ClassPathLepRepository {

    @SneakyThrows
    public Map<String, XmLepConfigFile> getLepFilesFromResources(String folderName) {
        Set<String> defaultLeps = getResourceFiles(folderName);
        Map<String, XmLepConfigFile> defauleLepsMap = new HashMap<>();
        for (String fileName: defaultLeps) {
            String content = IOUtils.toString(getClass().getResourceAsStream("/" + fileName), UTF_8);
            String relativePath = fileName.substring(folderName.length());
            defauleLepsMap.put(relativePath, new XmLepConfigFile(relativePath, content));
        }
        return defauleLepsMap;
    }

    private Set<String> getResourceFiles(String folderName) {
        Reflections reflections = new Reflections(folderName, new ResourcesScanner());
        return reflections.getResources(x -> true);
    }

}
