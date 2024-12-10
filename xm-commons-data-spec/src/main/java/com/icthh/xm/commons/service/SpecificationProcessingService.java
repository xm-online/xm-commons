package com.icthh.xm.commons.service;

import com.icthh.xm.commons.domain.SpecWithInputDataAndForm;
import com.icthh.xm.commons.domain.SpecWithDefinitionAndForm;

import java.util.Collection;

public interface SpecificationProcessingService<S extends SpecWithDefinitionAndForm> {

    /**
     * Method to process specification data
     * @param tenant            specification tenant
     * @param dataSpecKey       specification with definition and forms key
     * @param specification     specification with definition and forms
     * @return                  processed specification
     */
    default S processSpecification(String tenant, String dataSpecKey, S specification) {
        processDataSpecification(tenant, dataSpecKey, specification.getSpecifications());
        return specification;
    }

    /**
     * Method to process specification data
     * @param tenant            specification tenant
     * @param dataSpecKey       specification with definition and forms key
     * @param specifications    specifications with data input spec
     * @return                  processed specifications
     */
    <I extends SpecWithInputDataAndForm> Collection<I> processDataSpecification(String tenant, String dataSpecKey, Collection<I> specifications);

    /**
     * Update all related specifications by tenant
     * @param tenant            tenant name
     * @param dataSpecKey       specification with definition and forms key
     * @param specifications    specification with definition and forms
     */
    void updateByTenantState(String tenant, String dataSpecKey, Collection<S> specifications);

}
