package com.icthh.xm.commons.lep;

import com.icthh.xm.lep.api.LepManagerService;
import com.icthh.xm.lep.groovy.LepResourceKeyURLConnection;
import com.icthh.xm.lep.groovy.LepScriptResourceConnector;
import com.icthh.xm.lep.groovy.ScriptNameLepResourceKeyMapper;
import groovy.util.ResourceException;
import java.net.URLConnection;
import java.util.concurrent.atomic.AtomicBoolean;

public class XmLepScriptResourceConnector extends LepScriptResourceConnector {

    private final LepManagerService managerService;
    private final ScriptNameLepResourceKeyMapper mapper;

    public XmLepScriptResourceConnector(LepManagerService managerService,
                                        ScriptNameLepResourceKeyMapper mapper) {
        super(managerService, mapper);
        this.managerService = managerService;
        this.mapper = mapper;
    }

    @Override
    public URLConnection getResourceConnection(String scriptName) throws ResourceException {
        try {
            return new XmLepResourceKeyURLConnection(mapper.map(scriptName),
                    managerService.getResourceService(),
                    managerService);
        } catch (Exception e) {
            throw new ResourceException("Error while building "
                    + LepResourceKeyURLConnection.class.getSimpleName()
                    + ": " + e.getMessage(), e);
        }
    }
}
