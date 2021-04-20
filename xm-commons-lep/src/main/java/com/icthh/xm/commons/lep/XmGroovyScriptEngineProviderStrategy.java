package com.icthh.xm.commons.lep;

import com.icthh.xm.lep.api.LepManagerService;
import com.icthh.xm.lep.groovy.LazyGroovyScriptEngineProviderStrategy;
import com.icthh.xm.lep.groovy.LepScriptResourceConnector;
import com.icthh.xm.lep.groovy.ScriptNameLepResourceKeyMapper;
import groovy.util.ResourceConnector;
import org.springframework.beans.factory.BeanClassLoaderAware;

import java.net.URLConnection;

/**
 * The {@link XmGroovyScriptEngineProviderStrategy} class.
 */
public class XmGroovyScriptEngineProviderStrategy extends LazyGroovyScriptEngineProviderStrategy
    implements BeanClassLoaderAware {

    private final ScriptNameLepResourceKeyMapper resourceKeyMapper;
    private final String appName;

    private ClassLoader springClassLoader;

    public XmGroovyScriptEngineProviderStrategy(ScriptNameLepResourceKeyMapper resourceKeyMapper, String appName) {
        super(resourceKeyMapper);
        this.resourceKeyMapper = resourceKeyMapper;
        this.appName = appName;
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
        return new LepClassResourceConnector(new LepScriptResourceConnector(managerService, resourceKeyMapper),
                appName,
                managerService);
    }

}
