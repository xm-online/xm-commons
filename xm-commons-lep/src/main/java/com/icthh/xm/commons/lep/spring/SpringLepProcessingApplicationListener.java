package com.icthh.xm.commons.lep.spring;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepScriptConstants.BINDING_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepScriptConstants.BINDING_KEY_TENANT_CONTEXT;

import com.icthh.xm.lep.api.ContextScopes;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.lep.api.LepProcessingEvent;
import com.icthh.xm.lep.api.LepProcessingEvent.AfterExecutionEvent;
import com.icthh.xm.lep.api.LepProcessingEvent.AfterProcessingEvent;
import com.icthh.xm.lep.api.LepProcessingEvent.BeforeExecutionEvent;
import com.icthh.xm.lep.api.LepProcessingEvent.BeforeProcessingEvent;
import com.icthh.xm.lep.api.ScopedContext;
import com.icthh.xm.commons.lep.LepProcessingListenerAdapter;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.tenant.TenantContext;
import org.springframework.context.ApplicationListener;

/**
 * The {@link SpringLepProcessingApplicationListener} class.
 */
public abstract class SpringLepProcessingApplicationListener extends LepProcessingListenerAdapter
    implements ApplicationListener<ApplicationLepProcessingEvent> {

    private static <T> T getRequiredValue(ScopedContext threadContext, String name, Class<T> type) {
        T value = threadContext.getValue(name, type);
        if (value == null) {
            throw new IllegalStateException("LEP manager thread context doesn't have value for '" + name + "'");
        }
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void onApplicationEvent(ApplicationLepProcessingEvent event) {
        LepProcessingEvent lepProcessingEvent = event.getLepProcessingEvent();
        if (lepProcessingEvent instanceof BeforeProcessingEvent) {
            onBeforeProcessingEvent(BeforeProcessingEvent.class.cast(lepProcessingEvent));
        } else if (lepProcessingEvent instanceof BeforeExecutionEvent) {
            onBeforeExecutionEvent(BeforeExecutionEvent.class.cast(lepProcessingEvent));
        } else if (lepProcessingEvent instanceof AfterExecutionEvent) {
            onAfterExecutionEvent(AfterExecutionEvent.class.cast(lepProcessingEvent));
        } else if (lepProcessingEvent instanceof AfterProcessingEvent) {
            onAfterProcessingEvent(AfterProcessingEvent.class.cast(lepProcessingEvent));
        } else {
            onOtherEvent(lepProcessingEvent);
        }
    }

    /**
     * Init execution context for script variables bindings.
     *
     * @param event the BeforeExecutionEvent
     */
    @Override
    public void onBeforeExecutionEvent(BeforeExecutionEvent event) {
        LepManager manager = event.getSource();
        ScopedContext threadContext = manager.getContext(ContextScopes.THREAD);
        if (threadContext == null) {
            throw new IllegalStateException("LEP manager thread context doesn't initialized");
        }

        TenantContext tenantContext = getRequiredValue(threadContext, THREAD_CONTEXT_KEY_TENANT_CONTEXT,
                                                       TenantContext.class);
        XmAuthenticationContext authContext = getRequiredValue(threadContext, THREAD_CONTEXT_KEY_AUTH_CONTEXT,
                                                               XmAuthenticationContext.class);

        ScopedContext executionContext = manager.getContext(ContextScopes.EXECUTION);
        // add contexts
        executionContext.setValue(BINDING_KEY_TENANT_CONTEXT, tenantContext);
        executionContext.setValue(BINDING_KEY_AUTH_CONTEXT, authContext);

        bindExecutionContext(executionContext);
    }

    /**
     * Bind beans to LEP execution context.
     *
     * @param executionContext execution context to bind
     */
    protected abstract void bindExecutionContext(ScopedContext executionContext);

}
