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
     * Method to update all data spec configuration storage per tenant per specs
     * @param tenant            specification tenant
     * @param baseSpecKey       base specification key
     * @param allConfigs        all data specifications to be updated in storage
     *
     * If you're using this method inside a for or foreach loop, move it outside the loop and pass all configurations to it instead.
     */

    void fullUpdateStateByTenant(String tenant, String baseSpecKey, Collection<S> allConfigs);

    /**
     * Method to process data spec
     * @param tenant            specification tenant
     * @param baseSpecKey       base specification key
     * @param setter            input data spec object setter
     * @param getter            input data spec object getter
     */
    void processDataSpec(String tenant, String baseSpecKey, Consumer<String> setter, Supplier<String> getter);
}
