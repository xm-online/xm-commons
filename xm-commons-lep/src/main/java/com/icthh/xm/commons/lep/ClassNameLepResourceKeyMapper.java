package com.icthh.xm.commons.lep;

import com.icthh.xm.lep.api.ContextsHolder;
import com.icthh.xm.lep.api.LepResource;
import com.icthh.xm.lep.api.LepResourceDescriptor;
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
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.icthh.xm.commons.lep.XmLepResourceSubType.TENANT;
import static com.icthh.xm.lep.api.commons.UrlLepResourceKey.LEP_PROTOCOL;
import static java.nio.charset.StandardCharsets.UTF_8;

@RequiredArgsConstructor
public class ClassNameLepResourceKeyMapper implements ScriptNameLepResourceKeyMapper {

    private final static String FILE_EXTENSION = ".groovy";
    private final static String LEP_SUFFIX = "$$" + TENANT.getName() + FILE_EXTENSION;

    private final ScriptNameLepResourceKeyMapper mapper;
    private final String appName;
    private final ContextsHolder contextsHolder;
    private final LepResourceService resourceService;
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
        String classPrefix = String.join("/", List.of(tenantKey, appName, LEP_PROTOCOL));
        if (name.startsWith(classPrefix) && name.endsWith(FILE_EXTENSION)) {

            final String path = name.substring(classPrefix.length(), name.length() - FILE_EXTENSION.length());
            URLStreamHandler urlStreamHandler = new LepURLStreamHandler(contextsHolder, resourceService);
            String prefix = LEP_PROTOCOL + "://" + tenantKey;

            // While with cut path by $ for inner for classes support
            String currentPath = path;
            while(true) {
                String lepUrl = prefix + currentPath + LEP_SUFFIX;
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
