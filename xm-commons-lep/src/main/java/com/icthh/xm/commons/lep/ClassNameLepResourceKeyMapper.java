package com.icthh.xm.commons.lep;

import com.icthh.xm.lep.api.ContextsHolder;
import com.icthh.xm.lep.api.LepResource;
import com.icthh.xm.lep.api.LepResourceService;
import com.icthh.xm.lep.api.commons.StringLepResourceKey;
import com.icthh.xm.lep.api.commons.UrlLepResourceKey;
import com.icthh.xm.lep.groovy.ScriptNameLepResourceKeyMapper;
import com.icthh.xm.lep.script.InputStreamSupplier;
import groovy.util.ResourceConnector;
import groovy.util.ResourceException;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.List;

import static com.icthh.xm.commons.lep.XmLepResourceSubType.AROUND;
import static com.icthh.xm.commons.lep.XmLepResourceSubType.TENANT;
import static com.icthh.xm.lep.api.commons.UrlLepResourceKey.LEP_PROTOCOL;

@RequiredArgsConstructor
public class ClassNameLepResourceKeyMapper implements ScriptNameLepResourceKeyMapper {

    private final String FILE_EXTENSION = ".groovy";
    private final String LEP_SUFFIX = "$$" + TENANT.getName() + FILE_EXTENSION;

    private final ScriptNameLepResourceKeyMapper mapper;
    private final String appName;
    private final ContextsHolder contextsHolder;
    private final LepResourceService resourceService;

    @Override
    public String map(UrlLepResourceKey resourceKey) {
        return mapper.map(resourceKey);
    }

    @Override
    public UrlLepResourceKey map(String name) {
        if (name == null) {
            return null;
        }

        String tenantKey = LepContextUtils.getTenantKey(contextsHolder);
        String classPrefix = String.join("/", List.of(tenantKey, appName, LEP_PROTOCOL));
        if (name.startsWith(classPrefix) && name.endsWith(FILE_EXTENSION) && !name.contains("$")) {
            String path = name.substring(classPrefix.length(), name.length() - FILE_EXTENSION.length());
            name = LEP_PROTOCOL + "://" + tenantKey + path + LEP_SUFFIX;
        }
        return new UrlLepResourceKey(name, buildDefaultStreamHandler(contextsHolder, resourceService));
    }

    private static URLStreamHandler buildDefaultStreamHandler(ContextsHolder contextsHolder,
                                                              LepResourceService resourceService) {
        return new URLStreamHandler() {
            @Override
            protected URLConnection openConnection(URL url) {
                return buildDefaultUrlConnection(url, contextsHolder, resourceService);
            }
        };
    }

    private static URLConnection buildDefaultUrlConnection(URL url,
                                                           ContextsHolder contextsHolder,
                                                           LepResourceService resourceService) {
        return new URLConnection(url) {
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
        };
    }
}
