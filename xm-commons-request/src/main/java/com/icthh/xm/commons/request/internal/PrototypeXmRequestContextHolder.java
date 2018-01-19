package com.icthh.xm.commons.request.internal;

import com.icthh.xm.commons.request.XmPrivilegedRequestContext;
import com.icthh.xm.commons.request.XmRequestContext;
import com.icthh.xm.commons.request.XmRequestContextHolder;

/**
 * The {@link PrototypeXmRequestContextHolder} class.
 */
public class PrototypeXmRequestContextHolder implements XmRequestContextHolder {

    /**
     * {@inheritDoc}
     */
    @Override
    public XmRequestContext getContext() {
        return new DataAdapterXmRequestContext(XmRequestContextData.get());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public XmPrivilegedRequestContext getPrivilegedContext() {
        return new DataAdapterXmPrivilegedRequestContext(XmRequestContextData.get());
    }

}
