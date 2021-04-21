package com.icthh.xm.commons.lep;

import com.icthh.xm.lep.api.ContextsHolder;
import com.icthh.xm.lep.api.LepResource;
import com.icthh.xm.lep.api.LepResourceService;
import com.icthh.xm.lep.api.commons.UrlLepResourceKey;
import com.icthh.xm.lep.groovy.ScriptNameLepResourceKeyMapper;
import com.icthh.xm.lep.script.InputStreamSupplier;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.List;

import static com.icthh.xm.commons.lep.XmLepResourceSubType.TENANT;
import static com.icthh.xm.lep.api.commons.UrlLepResourceKey.LEP_PROTOCOL;

@RequiredArgsConstructor
public class ClassNameLepResourceKeyMapper implements ScriptNameLepResourceKeyMapper {

    private final static String FILE_EXTENSION = ".groovy";
    private final static String LEP_SUFFIX = "$$" + TENANT.getName() + FILE_EXTENSION;

    private final ScriptNameLepResourceKeyMapper mapper;
    private final String appName;
    private final ContextsHolder contextsHolder;
    private final LepResourceService resourceService;

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

            String path = name.substring(classPrefix.length(), name.length() - FILE_EXTENSION.length());
            URLStreamHandler urlStreamHandler = new LepURLStreamHandler(contextsHolder, resourceService);
            String prefix = LEP_PROTOCOL + "://" + tenantKey;

            // While with cut path by $ for inner for classes support
            while(true) {
                String lepUrl = prefix + path + LEP_SUFFIX;
                UrlLepResourceKey resourceKey = new UrlLepResourceKey(lepUrl, urlStreamHandler);
                if (resourceService.isResourceExists(this.contextsHolder, resourceKey)) {
                    return resourceKey;
                }

                if (path.lastIndexOf("$") <= 0) {
                    break;
                }

                path = path.substring(0, path.lastIndexOf("$"));
            }
        }
        return mapper.map(name);
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
