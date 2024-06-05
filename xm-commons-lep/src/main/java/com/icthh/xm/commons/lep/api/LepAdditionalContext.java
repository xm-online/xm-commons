package com.icthh.xm.commons.lep.api;

import com.icthh.xm.commons.lep.TargetProceedingLep;

import java.util.Optional;

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
    default Optional<T> additionalContextValue(BaseLepContext lepContext, LepEngine lepEngine, TargetProceedingLep lepMethod) {
        return Optional.empty();
    }
    Class<? extends LepAdditionalContextField> fieldAccessorInterface();

}
