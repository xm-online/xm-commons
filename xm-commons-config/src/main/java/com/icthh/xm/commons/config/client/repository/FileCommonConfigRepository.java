package com.icthh.xm.commons.config.client.repository;

import com.icthh.xm.commons.config.client.config.XmConfigProperties;
import com.icthh.xm.commons.config.domain.Configuration;
import com.icthh.xm.commons.exceptions.BusinessException;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.springframework.util.AntPathMatcher;

import static java.io.File.separator;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.FileUtils.listFiles;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.apache.commons.lang3.StringUtils.replaceChars;

@RequiredArgsConstructor
public class FileCommonConfigRepository implements CommonConfigRepository {

    private static final IOFileFilter TRUE_FILTER = TrueFileFilter.INSTANCE;

    private final XmConfigProperties xmConfigProperties;
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    @Override
    public Map<String, Configuration> getConfig(String commit) {
        File baseDir = new File(xmConfigProperties.getDirectoryBasePath());
        Collection<File> listFiles = listFiles(new File(baseDir, "config"), TRUE_FILTER, TRUE_FILTER);
        List<String> paths = listFiles.stream()
                                      .map(File::toPath)
                                      .map(path -> separator + baseDir.toPath().relativize(path))
                                      .toList();
        return getConfig(commit, paths);
    }

    @Override
    @SneakyThrows
    public Map<String, Configuration> getConfig(String version, Collection<String> paths) {
        File basePath = new File(xmConfigProperties.getDirectoryBasePath());
        Map<String, Configuration> configurationMap = new HashMap<>();
        for (String relativePath: paths) {
            String content = readFile(basePath, relativePath);
            String path = replaceChars(relativePath, separator, "/");
            configurationMap.put(path, new Configuration(path, content));
        }
        return configurationMap;
    }

    @Override
    public Map<String, Configuration> getConfigByPatternPaths(String version, Collection<String> patterns) {
        File basePath = new File(xmConfigProperties.getDirectoryBasePath());
        List<String> fullPatternPaths = patterns.stream()
                .map(pattern -> basePath.getPath() + pattern)
                .toList();

        List<String> filteredConfigPaths = listFiles(basePath, TRUE_FILTER, TRUE_FILTER).stream()
                .map(File::getPath)
                .filter(path -> matchPath(path, fullPatternPaths))
                .toList();

        Map<String, Configuration> configurationMap = new HashMap<>();

        for (String fullPath: filteredConfigPaths) {
            String content = readFile(new File(fullPath));
            String path = replaceChars(fullPath, separator, "/");
            configurationMap.put(path, new Configuration(path, content));
        }
        return configurationMap;
    }

    private boolean matchPath(String path, Collection<String> pathsAntPattern) {
        return pathsAntPattern.stream().anyMatch(it -> antPathMatcher.match(it, path));
    }

    @SneakyThrows
    private static String readFile(File basePath, String relativePath) {
        File file = new File(basePath, relativePath);
        return readFile(file);
    }

    @SneakyThrows
    private static String readFile(File file) {
        if (file.exists()) {
            return readFileToString(file, UTF_8);
        } else {
            return null;
        }
    }

    @Override
    @SneakyThrows
    public void updateConfigFullPath(Configuration configuration, String oldConfigHash) {
        File file = new File(xmConfigProperties.getDirectoryBasePath(), configuration.getPath());
        writeStringToFile(file, configuration.getContent(), UTF_8);
    }

}
