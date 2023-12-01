package com.icthh.xm.commons.lep.api;

/**
 * Implement this interface if you want to add custom field to root of lepContext.
 *
 *
 * IMPORTANT Note: Only for utils or commons! Implement LepServiceFactory in you what to extend lepContext.
 * @param <T> type of additional lepContext field
 */
public interface LepAdditionalContext<T> {

    String additionalContextKey();
    T additionalContextValue();
    Class<? extends LepAdditionalContextField> fieldAccessorInterface();

}
