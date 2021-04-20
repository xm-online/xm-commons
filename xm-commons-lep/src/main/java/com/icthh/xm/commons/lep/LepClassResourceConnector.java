package com.icthh.xm.commons.lep;

import com.icthh.xm.lep.api.ContextsHolder;
import groovy.util.ResourceConnector;
import groovy.util.ResourceException;
import lombok.RequiredArgsConstructor;

import java.net.URLConnection;
import java.util.List;

import static com.icthh.xm.lep.api.commons.UrlLepResourceKey.LEP_PROTOCOL;

@RequiredArgsConstructor
public class LepClassResourceConnector implements ResourceConnector {

    private final ResourceConnector resourceConnector;
    private final String appName;
    private final ContextsHolder contextsHolder;

    @Override
    public URLConnection getResourceConnection(String name) throws ResourceException {
        String tenantKey = LepContextUtils.getTenantKey(contextsHolder);
        String classPrefix = String.join("/", List.of(tenantKey, appName, LEP_PROTOCOL));
        if (name != null && name.startsWith(classPrefix)) {
            name = LEP_PROTOCOL + "://" + tenantKey + name.substring(classPrefix.length());
        }
        return resourceConnector.getResourceConnection(name);
    }
}
