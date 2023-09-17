package com.icthh.xm.commons.lep.groovy;

import com.icthh.xm.commons.config.client.service.TenantAliasService;
import com.icthh.xm.commons.lep.api.XmLepConfigFile;
import com.icthh.xm.commons.lep.groovy.GroovyFileParser.GroovyFileMetadata;
import com.icthh.xm.commons.lep.groovy.storage.LepConnectionCache;
import com.icthh.xm.commons.lep.groovy.storage.LepStorage;
import groovy.util.ResourceConnector;
import groovy.util.ResourceException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamSource;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.toSet;

@Slf4j
public class LepResourceConnector implements ResourceConnector {
    private static final String COMMONS = "commons";
    private static final String FILE_EXTENSION = ".groovy";
    private static final String LEP_SUFFIX = "$$tenant";
    private static final String LEP_URL_PREFIX = "lep://";
    private static final String LEP_FOLDER = "lep";
    public static final String URL_PREFIX_COMMONS_ENVIRONMENT = "commons/environment";
    public static final String URL_PREFIX_COMMONS_TENANT = "commons/tenant";

    private final GroovyFileParser groovyFileParser;

    private final String tenantKey;
    private final String appName;
    private final TenantAliasService tenantAliasService;
    private final LepStorage leps;
    private final Map<String, GroovyFileMetadata> lepMetadata = new ConcurrentHashMap<>();
    private final List<String> parentKeysOnCreateEngine;
    private final Set<String> lepPathPrefixes;
    private final LepConnectionCache lepConnectionCache;

    public LepResourceConnector(String tenantKey,
                                String appName,
                                TenantAliasService tenantAliasService,
                                LepStorage leps,
                                Map<String, GroovyFileMetadata> lepMetadata,
                                GroovyFileParser groovyFileParser) {
        this.tenantKey = tenantKey;
        this.appName = appName;
        this.tenantAliasService = tenantAliasService;
        this.leps = leps;
        this.lepConnectionCache = leps.buildCache();
        this.groovyFileParser = groovyFileParser;
        this.lepMetadata.putAll(lepMetadata);

        List<String> parentKeys = tenantAliasService.getTenantAliasTree().getParentKeys(tenantKey);
        this.parentKeysOnCreateEngine = parentKeys;
        this.lepPathPrefixes = buildLepPrefixes(tenantKey, appName, parentKeys);
    }

    private GroovyFileMetadata toFileMetaData(XmLepConfigFile lep) {
        return groovyFileParser.getFileMetaData(lep.readContent());
    }

    private static Set<String> buildLepPrefixes(String tenantKey, String appName, List<String> parentKeys) {
        Set<String> parentTenantPrefixes = parentKeys.stream()
            .flatMap(key -> Stream.of(
                key + "/" + appName + "/lep/",
                key + "/commons/lep/"
            )).collect(toSet());
        Set<String> prefixes = new HashSet<>();
        prefixes.addAll(parentTenantPrefixes);
        prefixes.addAll(Set.of(
            tenantKey + "/" + appName + "/lep/",
            tenantKey + "/commons/lep/",
            "commons/tenant/",
            "commons/environment/",
            "commons/lep/"
        ));
        return unmodifiableSet(prefixes);
    }


    @SneakyThrows
    public URLConnection getResourceConnection(final String url) {
        String name = url;
        if (log.isTraceEnabled()) {
            log.trace("Resolve import {}", name);
        }

        if (lepConnectionCache.isConnectionExists(url)) {
            return lepConnectionCache.getConnection(url);
        }

        if (name.startsWith(LEP_URL_PREFIX)) {
            name = name.substring(LEP_URL_PREFIX.length());
        }
        if (name.endsWith(FILE_EXTENSION)) {
            name = name.substring(0, name.length() - FILE_EXTENSION.length());
        }

        assertPrefix(name);

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
                    return lepConnectionCache.putConnection(url, connection::get);
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

        if (url.startsWith(LEP_URL_PREFIX)) {
            return lepConnectionCache.putConnection(url, () ->  toEmptyLepConnection(url));
        } else {
            var finalName = name;
            return lepConnectionCache.putConnection(url, () -> {
                throw new ResourceException("Resource not found " + finalName);
            });
        }
    }

    private void assertPrefix(String name) throws ResourceException {
        List<String> actualParentKeys = tenantAliasService.getTenantAliasTree().getParentKeys(tenantKey);
        if (lepPathPrefixes.stream().noneMatch(name::startsWith) && this.parentKeysOnCreateEngine.equals(actualParentKeys)) {
            if (log.isTraceEnabled()) {
                log.trace("Import {} it's not are lep file", name);
            }
            throw new ResourceException("Resource not found " + name);
        }
    }

    @SneakyThrows
    public Optional<URLConnection> toLepConnection(String path) {
        XmLepConfigFile xmLepConfigFile = leps.getByPath(path);
        if (xmLepConfigFile != null) {
            URL url = new URL(null, LEP_URL_PREFIX + path, new LepURLStreamHandler(xmLepConfigFile));
            return Optional.of(url.openConnection());
        } else {
            return Optional.empty();
        }
    }

    @SneakyThrows
    public URLConnection toEmptyLepConnection(String path) {
        URL url = new URL(null, LEP_URL_PREFIX + path, new EmptyLepURLStreamHandler(path));
        return url.openConnection();
    }

    @SneakyThrows
    public Optional<URLConnection> toLepConnection(String lepUrl, String path) {
        XmLepConfigFile xmLepConfigFile = leps.getByPath(lepUrl);
        if (xmLepConfigFile != null && containsClassDefinition(lepUrl, path, xmLepConfigFile)) {
            URL url = new URL(null, LEP_URL_PREFIX + lepUrl, new LepURLStreamHandler(xmLepConfigFile));
            return Optional.of(url.openConnection());
        } else {
            return Optional.empty();
        }
    }

    @SneakyThrows
    private boolean containsClassDefinition(String lepUrl, String path, XmLepConfigFile lepFile) {
        String lepPath = lepUrl;
        if (lepPath.endsWith(LEP_SUFFIX)) {
            lepPath = lepPath.substring(0, lepPath.length() - LEP_SUFFIX.length());
        }

        if (!path.startsWith(lepPath)) {
            return false;
        }

        String className = "";
        if (lepPath.lastIndexOf("/") > 0) {
            className = lepPath.substring(lepPath.lastIndexOf("/") + 1);
        }

        String importValue = path.substring(lepPath.length());
        importValue = className + importValue;
        var metadata = lepMetadata.computeIfAbsent(lepFile.metadataKey(), it -> toFileMetaData(lepFile));
        return metadata != null && metadata.canImport(importValue);
    }

    private Optional<LepRootPath> getLepBasePath(String name) {
        List<LepRootPath> rootPathVariants = new ArrayList<>();

        rootPathVariants.add(new LepRootPath(name, tenantKey + "/" + appName, tenantKey + "/" + appName));

        List<String> parentKeys = tenantAliasService.getTenantAliasTree()
            .getParentKeys(tenantKey);
        parentKeys.stream()
            .map(tenant -> tenant + "/" + appName)
            .map(it -> new LepRootPath(name, tenantKey + "/" + appName, it))
            .forEach(rootPathVariants::add);

        parentKeys.stream()
            .map(tenant -> tenant + "/" + COMMONS)
            .map(it -> new LepRootPath(name, tenantKey + "/" + COMMONS, it))
            .forEach(rootPathVariants::add);

        rootPathVariants.add(new LepRootPath(name, tenantKey + "/" + COMMONS + "/" + LEP_FOLDER, URL_PREFIX_COMMONS_TENANT));
        rootPathVariants.add(new LepRootPath(name, tenantKey + "/" + COMMONS, tenantKey + "/" + COMMONS));
        rootPathVariants.add(new LepRootPath(name, COMMONS + "/" + LEP_FOLDER, URL_PREFIX_COMMONS_ENVIRONMENT));
        rootPathVariants.add(new LepRootPath(name, COMMONS, COMMONS));

        return rootPathVariants.stream().filter(LepRootPath::isMatch).findFirst();
    }

    private static class LepURLStreamHandler extends URLStreamHandler {
        private final InputStreamSource content;
        private final long lastModified;

        public LepURLStreamHandler(XmLepConfigFile xmLepConfigFile) {
            this.content = xmLepConfigFile.getContentStream();
            this.lastModified = xmLepConfigFile.getLastModified();
        }

        protected URLConnection openConnection(URL u) {
            return new LepURLConnection(u, content, lastModified);
        }
    }

    private static class LepURLConnection extends URLConnection {
        private final InputStreamSource inputStream;
        private final long lastModified;

        protected LepURLConnection(URL url, InputStreamSource inputStream, long lastModified) {
            super(url);
            this.inputStream = inputStream;
            this.lastModified = lastModified;
        }

        @Override
        @SneakyThrows
        public InputStream getInputStream() {
            return inputStream.getInputStream();
        }

        @Override
        public long getLastModified() {
            return lastModified;
        }

        public void connect() {
        }
    }

    @RequiredArgsConstructor
    private static class EmptyLepURLStreamHandler extends URLStreamHandler {
        private final String url;

        @Override
        protected URLConnection openConnection(URL u) {
            return new EmptyLepURLConnection(u);
        }
    }

    private static class EmptyLepURLConnection extends URLConnection {

        protected EmptyLepURLConnection(URL url) {
            super(url);
        }

        @Override
        @SneakyThrows
        public InputStream getInputStream() {
            throw new ResourceException("Resource not found " + url);
        }

        @Override
        public long getLastModified() {
            return Instant.now().toEpochMilli();
        }

        @Override
        @SneakyThrows
        public void connect() {
            throw new ResourceException("Resource not found " + url);
        }
    }

    @ToString
    private static class LepRootPath {
        private final String name;
        private final String rootPath;
        private final String prefix;

        LepRootPath(String name, String rootPath, String prefix) {
            this.name = name;
            this.rootPath = rootPath;
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
