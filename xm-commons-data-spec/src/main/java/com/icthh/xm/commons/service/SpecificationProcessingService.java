package com.icthh.xm.commons.service;

import com.icthh.xm.commons.domain.BaseSpecification;
import com.icthh.xm.commons.domain.SpecificationItem;

import java.util.Collection;

public interface SpecificationProcessingService<S extends BaseSpecification> {

    /**
     * Method to process specification data
     * @param tenant            specification tenant
     * @param baseSpecKey       base specification key
     * @param specification     base specification with definition and forms
     * @return                  processed specification
     */
    default S processSpecification(String tenant, String baseSpecKey, S specification) {
        processDataSpecification(tenant, baseSpecKey, specification.getItems());
        return specification;
    }

    /**
     * Method to process specification data
     * @param tenant            specification tenant
     * @param baseSpecKey       base specification key
     * @param specifications    base specifications with data input spec
     * @return                  processed specifications
     */
    <I extends SpecificationItem> Collection<I> processDataSpecification(String tenant, String baseSpecKey, Collection<I> specifications);

    /**
     * Update all related specifications by tenant
     * @param tenant            tenant name
     * @param baseSpecKey       base specification key
     * @param specifications    base specification with definition and forms
     */
    void updateByTenantState(String tenant, String baseSpecKey, Collection<S> specifications);

}
