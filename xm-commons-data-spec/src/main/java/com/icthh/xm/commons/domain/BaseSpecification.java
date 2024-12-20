package com.icthh.xm.commons.domain;

import java.util.Collection;
import java.util.List;

public interface BaseSpecification extends HasDefinitions, HasForms {

    default <I extends SpecificationItem> Collection<I> getItems() {
        return List.of();
    }
}
