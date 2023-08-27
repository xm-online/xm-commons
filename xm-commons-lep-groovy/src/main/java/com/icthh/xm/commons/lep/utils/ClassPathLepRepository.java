package com.icthh.xm.commons.lep.utils;

import com.icthh.xm.commons.lep.api.XmLepConfigFile;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;

@Component
public class ClassPathLepRepository {

    @SneakyThrows
    public Map<String, XmLepConfigFile> getLepFilesFromResources() {
        Set<String> defaultLeps = getResourceFiles();
        Map<String, XmLepConfigFile> defauleLepsMap = new HashMap<>();
        for (String fileName: defaultLeps) {
            String content = IOUtils.toString(getClass().getResourceAsStream("/" + fileName), UTF_8);
            String relativePath = fileName.substring("lep/default".length());
            defauleLepsMap.put(relativePath, new XmLepConfigFile(relativePath, content));
        }
        return defauleLepsMap;
    }

    private Set<String> getResourceFiles() {
        Reflections reflections = new Reflections("lep/default", new ResourcesScanner());
        return reflections.getResources(x -> true);
    }

}
