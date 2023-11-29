package com.icthh.xm.commons.lep.impl.utils;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.icthh.xm.commons.lep.api.XmLepConfigFile;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.scanners.Scanner;
import org.reflections.scanners.Scanners;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


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
        Reflections reflections = new Reflections("lep", Scanners.Resources);
        return reflections.getResources(Pattern.compile(".*")).stream()
            .filter(it -> it.startsWith(folderName))
            .collect(Collectors.toSet());
    }

}
