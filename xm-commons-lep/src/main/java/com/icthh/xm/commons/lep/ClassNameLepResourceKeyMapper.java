package com.icthh.xm.commons.lep;

import com.icthh.xm.commons.config.client.service.TenantAliasService;
import com.icthh.xm.commons.config.domain.TenantAliasTree;
import com.icthh.xm.lep.api.ContextsHolder;
import com.icthh.xm.lep.api.LepResource;
import com.icthh.xm.lep.api.LepResourceService;
import com.icthh.xm.lep.api.commons.UrlLepResourceKey;
import com.icthh.xm.lep.groovy.ScriptNameLepResourceKeyMapper;
import com.icthh.xm.lep.script.InputStreamSupplier;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.icthh.xm.commons.lep.TenantScriptStorage.URL_PREFIX_COMMONS_ENVIRONMENT;
import static com.icthh.xm.commons.lep.TenantScriptStorage.URL_PREFIX_COMMONS_TENANT;
import static com.icthh.xm.commons.lep.XmLepResourceSubType.TENANT;
import static com.icthh.xm.lep.api.commons.UrlLepResourceKey.LEP_PROTOCOL;
import static java.nio.charset.StandardCharsets.UTF_8;

@RequiredArgsConstructor
public class ClassNameLepResourceKeyMapper implements ScriptNameLepResourceKeyMapper {

    private static final String COMMONS = "commons";
    private final static String FILE_EXTENSION = ".groovy";
    private final static String LEP_SUFFIX = "$$" + TENANT.getName() + FILE_EXTENSION;

    private final ScriptNameLepResourceKeyMapper mapper;
    private final String appName;
    private final ContextsHolder contextsHolder;
    private final LepResourceService resourceService;
    private final TenantAliasService tenantAliasService;
    private final GroovyFileParser groovyFileParser = new GroovyFileParser();

    @Override
    public String map(UrlLepResourceKey resourceKey) {
        return mapper.map(resourceKey);
    }

    @Override
    public UrlLepResourceKey map(final String name) {
        if (name == null) {
            return null;
        }

        String tenantKey = LepContextUtils.getTenantKey(contextsHolder);
        var optionalLepBasePath = getLepBasePath(name);
        if (optionalLepBasePath.isPresent()) {
            var lepBasePath = optionalLepBasePath.get();

            URLStreamHandler urlStreamHandler = new LepURLStreamHandler(contextsHolder, resourceService);

            // While with cut path by $ for inner for classes support
            String path = lepBasePath.getPath();
            String currentPath = path;
            while (true) {
                String lepUrl = currentPath + LEP_SUFFIX;
                UrlLepResourceKey resourceKey = new UrlLepResourceKey(lepUrl, urlStreamHandler);
                LepResource resource = resourceService.getResource(this.contextsHolder, resourceKey);
                if (resource != null && containsClassDefinition(resource, path)) {
                    return resourceKey;
                }

                if (currentPath.lastIndexOf("$") <= 0) {
                    break;
                }

                currentPath = currentPath.substring(0, currentPath.lastIndexOf("$"));
            }
        }
        return mapper.map(name);
    }

    private Optional<LepRootPath> getLepBasePath(String name) {
        if (!name.endsWith(FILE_EXTENSION)) {
            return Optional.empty();
        }

        String tenantKey = LepContextUtils.getTenantKey(contextsHolder);

        List<LepRootPath> rootPathVariants = new ArrayList<>();
        rootPathVariants.add(new LepRootPath(
                name, List.of(tenantKey, COMMONS, LEP_PROTOCOL), URL_PREFIX_COMMONS_TENANT));
        rootPathVariants.add(new LepRootPath(
                name, List.of(COMMONS, LEP_PROTOCOL), URL_PREFIX_COMMONS_ENVIRONMENT));

        List<List<String>> rootFoldersVariants = new ArrayList<>();
        rootFoldersVariants.add(List.of(tenantKey, appName, LEP_PROTOCOL));
        tenantAliasService.getTenantAliasTree()
                .getParents(tenantKey).stream()
                .map(TenantAliasTree.TenantAlias::getKey)
                .map(tenant -> List.of(tenant, appName, LEP_PROTOCOL))
                .forEach(rootFoldersVariants::add);
        rootFoldersVariants.stream().map(it -> new LepRootPath(name, it)).forEach(rootPathVariants::add);

        return rootPathVariants.stream().filter(LepRootPath::isMatch).findFirst();
    }

    @SneakyThrows
    private boolean containsClassDefinition(LepResource resource, String path) {
        if (resource instanceof InputStreamSupplier) {
            String className = path;
            if (path.lastIndexOf("$") > 0) {
                className = path.substring(path.lastIndexOf("$") + 1);
            } else if (path.lastIndexOf("/") > 0) {
                className = path.substring(path.lastIndexOf("/") + 1);
            }

            try (InputStream inputStream = ((InputStreamSupplier) resource).getInputStream()) {
                String content = StreamUtils.copyToString(inputStream, UTF_8);
                return groovyFileParser.isFileContainsClassDefinition(content, className);
            }
        }
        return false;
    }

    static class LepRootPath {
        private final String name;
        private final String rootPath;
        private final String prefix;

        LepRootPath(String name, List<String> rootPath) {
            this.name = name;
            this.rootPath = String.join("/", rootPath);
            this.prefix = LEP_PROTOCOL + "://" + rootPath.get(0);
        }

        LepRootPath(String name, List<String> rootPath, String prefix) {
            this.name = name;
            this.rootPath = String.join("/", rootPath);
            this.prefix = LEP_PROTOCOL + "://" + prefix;
        }

        public String getPath() {
            return prefix + name.substring(rootPath.length(), name.length() - FILE_EXTENSION.length());
        }

        public boolean isMatch() {
            return name.startsWith(rootPath);
        }
    }

    @RequiredArgsConstructor
    static class LepURLStreamHandler extends URLStreamHandler {
        private final ContextsHolder contextsHolder;
        private final LepResourceService resourceService;

        @Override
        protected URLConnection openConnection(URL url) {
            return new LepURLConnection(url, contextsHolder, resourceService);
        }
    }

    static class LepURLConnection extends URLConnection {
        private final URL url;
        private final ContextsHolder contextsHolder;
        private final LepResourceService resourceService;

        LepURLConnection(URL url, ContextsHolder contextsHolder, LepResourceService resourceService) {
            super(url);
            this.url = url;
            this.contextsHolder = contextsHolder;
            this.resourceService = resourceService;
        }

        @Override
        public void connect() throws IOException {
            throw new IOException("Cannot connect to " + this.url);
        }

        @Override
        public InputStream getInputStream() throws IOException {
            LepResource resource = resourceService.getResource(contextsHolder, new UrlLepResourceKey(url.toString()));
            if (resource instanceof InputStreamSupplier) {
                return ((InputStreamSupplier) resource).getInputStream();
            }
            return super.getInputStream();
        }
    }
}
