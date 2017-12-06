package com.icthh.xm.commons.lep.spring;

import com.icthh.xm.lep.api.ExtensionService;
import com.icthh.xm.lep.api.LepExecutor;
import com.icthh.xm.lep.api.LepInvocationCauseException;
import com.icthh.xm.lep.api.LepKey;
import com.icthh.xm.lep.api.LepKeyResolver;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.lep.api.LepManagerListener;
import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.lep.api.LepProcessingListener;
import com.icthh.xm.lep.api.LepResourceService;
import com.icthh.xm.lep.api.ScopedContext;
import com.icthh.xm.lep.api.Version;
import com.icthh.xm.lep.core.CoreLepManager;
import com.icthh.xm.commons.lep.XmLepLoggingExecutorListener;

import java.util.function.Consumer;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * The {@link SpringLepManager} class.
 */
public class SpringLepManager implements LepManager {

    private final CoreLepManager delegate = new CoreLepManager();
    private final ExtensionService extensionService;
    private final LepExecutor executor;
    private final ApplicationLepProcessingEventPublisher lepProcessingEventPublisher;
    private final LepResourceService resourceService;
    private final XmLepLoggingExecutorListener xmLepLoggingExecutorListener = new XmLepLoggingExecutorListener();

    public SpringLepManager(ExtensionService extensionService,
                            LepExecutor executor,
                            ApplicationLepProcessingEventPublisher lepProcessingEventPublisher,
                            LepResourceService resourceService) {
        this.extensionService = extensionService;
        this.executor = executor;
        this.lepProcessingEventPublisher = lepProcessingEventPublisher;
        this.resourceService = resourceService;
    }

    @PostConstruct
    public void init() {
        init(extensionService, resourceService, executor);
        delegate.registerProcessingListener(lepProcessingEventPublisher);
        executor.registerExecutorListener(xmLepLoggingExecutorListener);
    }

    @Override
    public void init(ExtensionService extensionService, LepResourceService resourceService, LepExecutor executor) {
        delegate.init(extensionService, resourceService, executor);
    }

    @PreDestroy
    @Override
    public void destroy() {
        executor.unregisterExecutorListener(xmLepLoggingExecutorListener);
        delegate.unregisterProcessingListener(lepProcessingEventPublisher);
        delegate.destroy();
    }

    @Override
    public void beginThreadContext(Consumer<? super ScopedContext> contextInitAction) {
        delegate.beginThreadContext(contextInitAction);
    }

    @Override
    public void endThreadContext() {
        delegate.endThreadContext();
    }

    @Override
    public Object processLep(LepKey key, Version extensionResourceVersion, LepKeyResolver keyResolver, LepMethod method)
    throws LepInvocationCauseException {
        return delegate.processLep(key, extensionResourceVersion, keyResolver, method);
    }

    @Override
    public boolean registerProcessingListener(LepProcessingListener listener) {
        return delegate.registerProcessingListener(listener);
    }

    @Override
    public boolean unregisterProcessingListener(LepProcessingListener listener) {
        return delegate.unregisterProcessingListener(listener);
    }

    @Override
    public boolean registerLepManagerListener(LepManagerListener listener) {
        return delegate.registerLepManagerListener(listener);
    }

    @Override
    public boolean unregisterLepManagerListener(LepManagerListener listener) {
        return delegate.unregisterLepManagerListener(listener);
    }

    @Override
    public ExtensionService getExtensionService() {
        return delegate.getExtensionService();
    }

    @Override
    public LepResourceService getResourceService() {
        return delegate.getResourceService();
    }

    @Override
    public ScopedContext getContext(String scope) {
        return delegate.getContext(scope);
    }

}
