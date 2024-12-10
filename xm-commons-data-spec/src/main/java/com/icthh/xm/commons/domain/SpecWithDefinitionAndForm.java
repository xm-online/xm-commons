package com.icthh.xm.commons.domain;

import java.util.Collection;
import java.util.List;

/**
 * Implement this class to add your specification to data spec processing
 */
public interface SpecWithDefinitionAndForm {

    default List<DefinitionSpec> getDefinitions() {
        return null; // null to ignore in json
    }

    default List<FormSpec> getForms() {
        return null; // null to ignore in json
    }

    <I extends SpecWithInputDataAndForm> Collection<I> getSpecifications();
}
