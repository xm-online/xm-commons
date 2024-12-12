package com.icthh.xm.commons.domain;

import java.util.Collection;

public interface BaseSpecification extends HasDefinitions, HasForms {

    <I extends SpecificationItem> Collection<I> getItems();
}
