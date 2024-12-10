package com.icthh.xm.commons.processor;

import com.icthh.xm.commons.domain.DataSpec;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Processor for specifications containing input data specs (e.g. definitions, forms or your custom input data spec)
 * @param <S>   input data specification for processing
 */
public interface ISpecProcessor<S extends DataSpec> {

    /**
     * Method to update data spec configuration storage per tenant per spec
     * @param tenant            specification tenant
     * @param dataSpecKey       specification with definition and forms key
     * @param configs           data specifications to be updated in storage
     */
    void updateStateByTenant(String tenant, String dataSpecKey, Collection<S> configs);

    /**
     * Method to process data spec
     * @param tenant            specification tenant
     * @param dataSpecKey       specification with definition and forms key
     * @param setter            input data spec object setter
     * @param getter            input data spec object getter
     */
    void processDataSpec(String tenant, String dataSpecKey, Consumer<String> setter, Supplier<String> getter);
}
