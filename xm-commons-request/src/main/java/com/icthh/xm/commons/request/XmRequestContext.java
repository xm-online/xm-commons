package com.icthh.xm.commons.request;

/**
 * Context holder for request-specific state, like current request data, current locale,
 * current theme, and potential binding errors.
 * <br>
 * Suitable in cases where Spring RequestContext doesn't meet the requirements of application.
 * For example, when application has no HttServletRequest or has not for all requests sources.
 */
public interface XmRequestContext {

    /**
     * Checks is key exist in current context.
     *
     * @param key the unique key of context value object
     * @return {@code true} if specified key exist in current context
     */
    boolean containsKey(String key);

    /**
     * Gets value from current context by key and cast it to specified type.
     *
     * @param key  the unique key of context value object
     * @param type the class type of the value
     * @param <T>  type of the value
     * @return the value object associated with key
     */
    <T> T getValue(String key, Class<T> type);

    /**
     * Gets value from current context by key and cast it to specified type
     * or return default value if context value is {@code null}.
     *
     * @param key          the unique key of context value object
     * @param type         the class type of the value
     * @param defaultValue the default value to be returned if context value is {@code null}
     * @param <T>          type of the value
     * @return the value object associated with key or default value if context value is {@code null}
     */
    <T> T getValueOrDefault(String key, Class<T> type, T defaultValue);

}
