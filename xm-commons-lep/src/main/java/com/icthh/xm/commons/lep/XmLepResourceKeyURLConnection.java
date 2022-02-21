package com.icthh.xm.commons.lep;

import com.icthh.xm.lep.api.ContextsHolder;
import com.icthh.xm.lep.api.LepResourceService;
import com.icthh.xm.lep.api.commons.UrlLepResourceKey;
import com.icthh.xm.lep.groovy.LepResourceKeyURLConnection;

import java.time.Instant;

public class XmLepResourceKeyURLConnection extends LepResourceKeyURLConnection {


    private final LepResourceService resourceService;
    private final UrlLepResourceKey resourceKey;
    private final ContextsHolder contextsHolder;

    /**
     * Constructs a URLConnection subclass to the specified URL.
     *
     * @param resourceKey     LEP resource key URL instance
     * @param resourceService LEP resource service
     * @param contextsHolder  LEP contexts holder
     */
    public XmLepResourceKeyURLConnection(UrlLepResourceKey resourceKey, LepResourceService resourceService,
                                         ContextsHolder contextsHolder) {
        super(resourceKey, resourceService, contextsHolder);
        this.resourceService = resourceService;
        this.resourceKey = resourceKey;
        this.contextsHolder = contextsHolder;
    }

    @Override
    public long getLastModified() {
        var resourceDescriptor = resourceService.getResourceDescriptor(contextsHolder, resourceKey);
        return resourceDescriptor == null ? Instant.now().toEpochMilli() : resourceDescriptor.getModificationTime().toEpochMilli();
    }
}
