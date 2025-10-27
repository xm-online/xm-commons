package com.icthh.xm.commons.config.client.repository;

import com.icthh.xm.commons.config.client.config.XmConfigProperties;
import com.icthh.xm.commons.config.domain.Configuration;
import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        return getConfigMap(basePath, paths);
    }

    @Override
    public Map<String, Configuration> getConfigByPatternPaths(String version, Collection<String> patterns) {
        File basePath = new File(xmConfigProperties.getDirectoryBasePath());

        List<String> filteredConfigPaths = listFiles(basePath, TRUE_FILTER, TRUE_FILTER).stream()
            .map(File::toPath)
            .map(path -> basePath.toPath().relativize(path))
            .map(Path::toString)
            .map(p -> separator + p)
            .filter(path -> matchPath(path, patterns))
            .toList();

        return getConfigMap(basePath, filteredConfigPaths);
    }

    private Map<String, Configuration> getConfigMap(File basePath, Collection<String> paths) {
        Map<String, Configuration> configurationMap = new HashMap<>();

        for (String path: paths) {
            String content = readFile(basePath, path);
            String normalizePath = replaceChars(path, separator, "/");
            configurationMap.put(normalizePath, new Configuration(normalizePath, content));
        }

        return configurationMap;
    }

    private boolean matchPath(String path, Collection<String> pathsAntPattern) {
        return pathsAntPattern.stream().anyMatch(it -> antPathMatcher.match(it, path));
    }

    @SneakyThrows
    private static String readFile(File basePath, String relativePath) {
        File file = new File(basePath, relativePath);
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
