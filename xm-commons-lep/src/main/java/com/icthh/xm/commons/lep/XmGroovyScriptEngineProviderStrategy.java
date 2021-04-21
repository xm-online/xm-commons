package com.icthh.xm.commons.lep;

import com.icthh.xm.lep.api.LepManagerService;
import com.icthh.xm.lep.api.LepResourceService;
import com.icthh.xm.lep.groovy.LazyGroovyScriptEngineProviderStrategy;
import com.icthh.xm.lep.groovy.LepScriptResourceConnector;
import com.icthh.xm.lep.groovy.ScriptNameLepResourceKeyMapper;
import groovy.util.ResourceConnector;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.core.io.ResourceLoader;

import java.net.URLConnection;

/**
 * The {@link XmGroovyScriptEngineProviderStrategy} class.
 */
public class XmGroovyScriptEngineProviderStrategy extends LazyGroovyScriptEngineProviderStrategy
    implements BeanClassLoaderAware {

    private final ScriptNameLepResourceKeyMapper resourceKeyMapper;
    private final String appName;
    private final LepResourceService resourceService;

    private ClassLoader springClassLoader;

    public XmGroovyScriptEngineProviderStrategy(ScriptNameLepResourceKeyMapper resourceKeyMapper,
                                                String appName,
                                                LepResourceService resourceService) {
        super(resourceKeyMapper);
        this.resourceKeyMapper = resourceKeyMapper;
        this.appName = appName;
        this.resourceService = resourceService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ClassLoader getParentClassLoader() {
        return springClassLoader;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.springClassLoader = classLoader;
    }

    @Override
    protected ResourceConnector buildResourceConnector(LepManagerService managerService) {
        return new LepScriptResourceConnector(managerService,
                new ClassNameLepResourceKeyMapper(resourceKeyMapper, appName, managerService, resourceService));
    }

}
