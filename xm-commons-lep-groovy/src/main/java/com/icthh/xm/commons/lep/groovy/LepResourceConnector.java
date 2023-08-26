package com.icthh.xm.commons.lep.groovy;

import com.icthh.xm.commons.config.client.service.TenantAliasService;
import com.icthh.xm.commons.config.domain.TenantAliasTree;
import com.icthh.xm.commons.lep.api.XmLepConfigFile;
import com.icthh.xm.commons.lep.groovy.GroovyFileParser.GroovyFileMetadata;
import groovy.util.ResourceConnector;
import groovy.util.ResourceException;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static com.icthh.xm.commons.lep.TenantScriptStorage.URL_PREFIX_COMMONS_ENVIRONMENT;
import static com.icthh.xm.commons.lep.TenantScriptStorage.URL_PREFIX_COMMONS_TENANT;

@Slf4j
public class LepResourceConnector implements ResourceConnector {
    private static final String COMMONS = "commons";
    private static final String FILE_EXTENSION = ".groovy";
    private static final String LEP_SUFFIX = "$$tenant";
    private static final String LEP_URL_PREFIX = "lep://";
    private static final String LEP_FOLDER = "lep";

    private final GroovyFileParser groovyFileParser = new GroovyFileParser();

    private final String tenantKey;
    private final String appName;
    private final TenantAliasService tenantAliasService;
    private final Map<String, XmLepConfigFile> leps;
    private final Map<String, GroovyFileMetadata> lepMetadata;

    private final Set<String> lepPathPrefixes;

    public LepResourceConnector(String tenantKey, String appName, TenantAliasService tenantAliasService, Map<String, XmLepConfigFile> leps) {
        this.tenantKey = tenantKey;
        this.appName = appName;
        this.tenantAliasService = tenantAliasService;
        this.leps = leps;
        Map<String, GroovyFileMetadata> lepMetadata = new HashMap<>();
        leps.forEach((path, lep) -> lepMetadata.put(path, groovyFileParser.getFileMetaData(lep.getContent())));
        this.lepMetadata = lepMetadata;

        this.lepPathPrefixes = Set.of(
            tenantKey + "/" + appName + "/lep/",
            tenantKey + "/commons/lep/",
            "commons/tenant/",
            "commons/environment/",
            "commons/lep/"
        );
    }


    @SneakyThrows
    public URLConnection getResourceConnection(String name) {
        if (log.isTraceEnabled()) {
            log.trace("Resolve import {}", name);
        }

        if (name.startsWith(LEP_URL_PREFIX)) {
            name = name.substring(LEP_URL_PREFIX.length());
        }
        if (name.endsWith(FILE_EXTENSION)) {
            name = name.substring(0, name.length() - FILE_EXTENSION.length());
        }

        if (lepPathPrefixes.stream().noneMatch(name::startsWith)) {
            if (log.isTraceEnabled()) {
                log.trace("Import {} it's not are lep file", name);
            }
            throw new ResourceException("Resource not found " + name);
        }

        var optionalLepBasePath = getLepBasePath(name);
        if (optionalLepBasePath.isPresent()) {
            var lepBasePath = optionalLepBasePath.get();

            // While with cut path by $ for inner for classes support
            String path = lepBasePath.getPath();
            String currentPath = path;
            while (true) {

                var connections = List.of(
                    toLepConnection(path),
                    toLepConnection(currentPath + LEP_SUFFIX, path),
                    toLepConnection(currentPath, path)
                );
                var connection = connections.stream().filter(Optional::isPresent).map(Optional::get).findFirst();
                if (connection.isPresent()) {
                    var resolvedFileUrl = connection.get().getURL().toString();
                    if (log.isTraceEnabled()) {
                        log.trace("Resolved {} import at {}", name, resolvedFileUrl);
                    }
                    System.out.println("Resolved " + name + " " + resolvedFileUrl);
                    return connection.get();
                }

                if (currentPath.lastIndexOf("$") <= 0) {
                    break;
                }

                currentPath = currentPath.substring(0, currentPath.lastIndexOf("$"));
            }
        }

        if (log.isTraceEnabled()) {
            log.trace("Resource {} not found", name);
        }
        throw new ResourceException("Resource not found " + name);
    }

    @SneakyThrows
    public Optional<URLConnection> toLepConnection(String path) {
        XmLepConfigFile xmLepConfigFile = leps.get(path);
        if (xmLepConfigFile != null) {
            String content = xmLepConfigFile.getContent();
            URL url = new URL(null, LEP_URL_PREFIX + path, new LepURLStreamHandler(content));
            return Optional.of(url.openConnection());
        } else {
            return Optional.empty();
        }
    }

    @SneakyThrows
    public Optional<URLConnection> toLepConnection(String lepUrl, String path) {
        XmLepConfigFile xmLepConfigFile = leps.get(lepUrl);
        if (xmLepConfigFile != null && containsClassDefinition(lepUrl, path)) {
            String content = xmLepConfigFile.getContent();
            URL url = new URL(null, LEP_URL_PREFIX + lepUrl, new LepURLStreamHandler(content));
            return Optional.of(url.openConnection());
        } else {
            return Optional.empty();
        }
    }

    @SneakyThrows
    private boolean containsClassDefinition(String lepUrl, String path) {
        if (!path.startsWith(lepUrl)) {
            return false;
        }

        String className = "";
        if (lepUrl.lastIndexOf("/") > 0) {
            className = lepUrl.substring(lepUrl.lastIndexOf("/") + 1);
        }

        String importValue = path.substring(lepUrl.length());
        importValue = className + importValue;
        var metadata = lepMetadata.get(lepUrl);
        return metadata != null && metadata.canImport(importValue);
    }

    private Optional<LepRootPath> getLepBasePath(String name) {
        List<LepRootPath> rootPathVariants = new ArrayList<>();
        rootPathVariants.add(new LepRootPath(name, List.of(tenantKey, COMMONS, LEP_FOLDER), URL_PREFIX_COMMONS_TENANT));
        rootPathVariants.add(new LepRootPath(name, List.of(COMMONS, LEP_FOLDER), URL_PREFIX_COMMONS_ENVIRONMENT));
        rootPathVariants.add(new LepRootPath(name, List.of(tenantKey, COMMONS), tenantKey + "/" + COMMONS));
        rootPathVariants.add(new LepRootPath(name, List.of(COMMONS), COMMONS));

        List<List<String>> rootFoldersVariants = new ArrayList<>();
        rootFoldersVariants.add(List.of(tenantKey, appName));
        tenantAliasService.getTenantAliasTree()
            .getParents(tenantKey).stream()
            .map(TenantAliasTree.TenantAlias::getKey)
            .map(tenant -> List.of(tenant, appName))
            .forEach(rootFoldersVariants::add);
        rootFoldersVariants.stream().map(it -> new LepRootPath(name, it)).forEach(rootPathVariants::add);

        return rootPathVariants.stream().filter(LepRootPath::isMatch).findFirst();
    }

    private static class LepURLStreamHandler extends URLStreamHandler {
        private final String content;

        public LepURLStreamHandler(String content) {
            this.content = content;
        }

        protected URLConnection openConnection(URL u) {
            return new LepURLConnection(u, content);
        }
    }

    @Getter
    private static class LepURLConnection extends URLConnection {
        private final InputStream inputStream;

        protected LepURLConnection(URL url, String content) {
            super(url);
            this.inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        }

        public void connect() {
        }
    }

    @ToString
    private static class LepRootPath {
        private final String name;
        private final String rootPath;
        private final String prefix;

        LepRootPath(String name, List<String> rootPath) {
            this.name = name;
            this.rootPath = String.join("/", rootPath);
            this.prefix = this.rootPath;
        }

        LepRootPath(String name, List<String> rootPath, String prefix) {
            this.name = name;
            this.rootPath = String.join("/", rootPath);
            this.prefix = prefix;
        }

        public String getPath() {
            return rootPath + name.substring(prefix.length());
        }

        public boolean isMatch() {
            return name.startsWith(prefix);
        }
    }
}
