package com.icthh.xm.commons.lep.impl.utils;

import com.icthh.xm.commons.lep.api.XmLepConfigFile;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class ClassPathLepRepository {

    private final ResourceLoader resourceLoader;

    @SneakyThrows
    public Map<String, XmLepConfigFile> getLepFilesFromResources(String folderName) {
        Set<String> defaultLeps = getResourceFiles(folderName);
        Map<String, XmLepConfigFile> defauleLepsMap = new HashMap<>();
        for (String fileName: defaultLeps) {
            Resource resource = resourceLoader.getResource("classpath:" + fileName);
            String relativePath = fileName.substring(folderName.length());
            defauleLepsMap.put(relativePath, new XmLepConfigFile(relativePath, resource));
        }
        return defauleLepsMap;
    }

    private Set<String> getResourceFiles(String folderName) {
        Reflections reflections = new Reflections(folderName, new ResourcesScanner());
        return  reflections.getResources(Pattern.compile(".*"));
    }

}
