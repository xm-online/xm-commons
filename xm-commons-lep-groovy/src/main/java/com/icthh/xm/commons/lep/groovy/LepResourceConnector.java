package com.icthh.xm.commons.lep.groovy;

import com.icthh.xm.commons.config.client.service.TenantAliasService;
import com.icthh.xm.commons.config.domain.TenantAliasTree;
import com.icthh.xm.commons.lep.api.XmLepConfigFile;
import groovy.util.ResourceConnector;
import groovy.util.ResourceException;
import lombok.SneakyThrows;

import java.io.ByteArrayInputStream;
import java.io.IOException;
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

public class LepResourceConnector implements ResourceConnector {
    private static final String COMMONS = "commons";
    private static final String FILE_EXTENSION = ".groovy";
    private static final String LEP_SUFFIX = "$$tenant" + FILE_EXTENSION;
    private static final String LEP_URL_PREFIX = "lep://";

    private final GroovyFileParser groovyFileParser = new GroovyFileParser();

    private final String tenantKey;
    private final String appName;
    private final TenantAliasService tenantAliasService;
    private final Map<String, XmLepConfigFile> leps;
    private final Map<String, Set<String>> lepClasses;

    public LepResourceConnector(String tenantKey, String appName, TenantAliasService tenantAliasService, Map<String, XmLepConfigFile> leps) {
        this.tenantKey = tenantKey;
        this.appName = appName;
        this.tenantAliasService = tenantAliasService;
        this.leps = leps;
        Map<String, Set<String>> lepClasses = new HashMap<>();
        leps.forEach((path, lep) -> lepClasses.put(path, groovyFileParser.getFileClassDefinition(lep.getContent())));
        this.lepClasses = lepClasses;
    }


    @SneakyThrows
    public URLConnection getResourceConnection(String name) {
        if (name.startsWith(LEP_URL_PREFIX)) {
            name = name.substring(LEP_URL_PREFIX.length());
        }

        String path = name.substring(0, name.length() - ".groovy".length());
        String currentPath = path;
        while (true) {

            var connections = Stream.of(
                toLepConnection(currentPath + LEP_SUFFIX, path),
                toLepConnection(currentPath, path)
            ).filter(Optional::isPresent).map(Optional::get).findFirst();
            if (connections.isPresent()) {
                return connections.get();
            }

            if (currentPath.lastIndexOf("$") <= 0) {
                break;
            }

            currentPath = currentPath.substring(0, currentPath.lastIndexOf("$"));
        }

        throw new ResourceException("Resource not found " + name);
    }

    @SneakyThrows
    public Optional<URLConnection> toLepConnection(String lepUrl, String path) {
        XmLepConfigFile xmLepConfigFile = leps.get(lepUrl);
        if (xmLepConfigFile != null && containsClassDefinition(lepUrl, path)) {
            String content = xmLepConfigFile.getContent();
            URL url = new URL(null, "lep:/" + lepUrl, new LepURLStreamHandler(content));
            return Optional.of(url.openConnection());
        } else {
            return Optional.empty();
        }
    }

    @SneakyThrows
    private boolean containsClassDefinition(String lepUrl, String path) {
        String className = path;
        if (path.lastIndexOf("$") > 0) {
            className = className.substring(path.lastIndexOf("$") + 1);
        } else if (path.lastIndexOf("/") > 0) {
            className = className.substring(className.lastIndexOf("/") + 1);
        }
        Set<String> classes = lepClasses.get(lepUrl);
        return classes != null && classes.contains(className);
    }

    private Optional<LepRootPath> getLepBasePath(String name) {
        if (!name.endsWith(FILE_EXTENSION)) {
            return Optional.empty();
        }

        List<LepRootPath> rootPathVariants = new ArrayList<>();
        rootPathVariants.add(new LepRootPath(
            name, List.of(tenantKey, COMMONS, LEP_URL_PREFIX), URL_PREFIX_COMMONS_TENANT));
        rootPathVariants.add(new LepRootPath(
            name, List.of(COMMONS, LEP_URL_PREFIX), URL_PREFIX_COMMONS_ENVIRONMENT));

        List<List<String>> rootFoldersVariants = new ArrayList<>();
        rootFoldersVariants.add(List.of(tenantKey, appName, LEP_URL_PREFIX));
        tenantAliasService.getTenantAliasTree()
            .getParents(tenantKey).stream()
            .map(TenantAliasTree.TenantAlias::getKey)
            .map(tenant -> List.of(tenant, appName, LEP_URL_PREFIX))
            .forEach(rootFoldersVariants::add);
        rootFoldersVariants.stream().map(it -> new LepRootPath(name, it)).forEach(rootPathVariants::add);

        return rootPathVariants.stream().filter(LepRootPath::isMatch).findFirst();
    }

    private static class LepURLStreamHandler extends URLStreamHandler {
        private final String content;

        public LepURLStreamHandler(String content) {
            this.content = content;
        }

        protected URLConnection openConnection(URL u) throws IOException {
            return new LepURLConnection(u, content);
        }
    }

    private static class LepURLConnection extends URLConnection {
        private final InputStream inputStream;

        protected LepURLConnection(URL url, String content) {
            super(url);
            this.inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        }

        public void connect() throws IOException {
        }

        public InputStream getInputStream() throws IOException {
            return this.inputStream;
        }
    }


    private static class LepRootPath {
        private final String name;
        private final String rootPath;
        private final String prefix;

        LepRootPath(String name, List<String> rootPath) {
            this.name = name;
            this.rootPath = String.join("/", rootPath);
            this.prefix = LEP_URL_PREFIX + "://" + rootPath.get(0);
        }

        LepRootPath(String name, List<String> rootPath, String prefix) {
            this.name = name;
            this.rootPath = String.join("/", rootPath);
            this.prefix = LEP_URL_PREFIX + "://" + prefix;
        }

        public String getPath() {
            return prefix + name.substring(rootPath.length());
        }

        public boolean isMatch() {
            return name.startsWith(rootPath);
        }
    }
}
