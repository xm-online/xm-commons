package com.icthh.xm.commons.request;

/**
 * The {@link XmPrivilegedRequestContext} extends {@link XmRequestContext},
 * adds operations to change context values and destroy current context itself.
 */
public interface XmPrivilegedRequestContext extends XmRequestContext {

    /**
     * Puts value to current context (in thread local implementation this values bound to current thread).
     *
     * @param key   the unique key of context value object
     * @param value the value object to be associated with key
     */
    void putValue(String key, Object value);

    /**
     * Destroys all values of current context.
     */
    void destroyCurrentContext();

}
