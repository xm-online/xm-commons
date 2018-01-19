package com.icthh.xm.commons.request;

/**
 * Single point to hold and access to request context objects.
 */
public interface XmRequestContextHolder {

    /**
     * Gets request context to read context values.
     *
     * @return the XmRequestContext instance
     */
    XmRequestContext getContext();

    /**
     * Gets extended request context with privileged operations to change context values.
     *
     * @return the XmPrivilegedRequestContext instance
     */
    XmPrivilegedRequestContext getPrivilegedContext();

}
