package com.icthh.xm.commons.lep.utils;

import com.icthh.xm.commons.lep.api.XmLepConfigFile;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;

@Component
public class ClassPathLepRepository {

    private final ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();

    @SneakyThrows
    public Map<String, XmLepConfigFile> getLepFilesFromResources() {
        Set<String> defaultLeps = getResourcesInDirectory("");
        Map<String, XmLepConfigFile> defauleLepsMap = new HashMap<>();
        for (String fileName: defaultLeps) {
            String content = IOUtils.toString(getClass().getResourceAsStream("/lep/default" + fileName), UTF_8);
            defauleLepsMap.put(fileName, new XmLepConfigFile(fileName, content));
        }
        return defauleLepsMap;
    }

    private Set<String> getResourcesInDirectory(String lepPath) throws IOException {
        Set<String> fileNames = new HashSet<>();
        Resource[] resources = resourcePatternResolver.getResources("classpath*:/lep/default" + lepPath + "/*");
        for (Resource resource : resources) {
            if (!resource.isReadable()) {
                fileNames.addAll(getResourcesInDirectory(lepPath + "/" + resource.getFilename()));
            } else {
                fileNames.add(lepPath + "/" + resource.getFilename());
            }
        }
        return fileNames;
    }
}
