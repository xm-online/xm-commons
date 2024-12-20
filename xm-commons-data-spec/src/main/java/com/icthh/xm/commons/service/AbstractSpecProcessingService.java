package com.icthh.xm.commons.service;

import com.icthh.xm.commons.domain.BaseSpecification;
import com.icthh.xm.commons.domain.SpecificationItem;
import com.icthh.xm.commons.processor.impl.DefinitionSpecProcessor;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RequiredArgsConstructor
public abstract class AbstractSpecProcessingService<S extends BaseSpecification> implements SpecificationProcessingService<S> {

    private final DefinitionSpecProcessor definitionSpecProcessor;

    @Override
    public <I extends SpecificationItem> Collection<I> processDataSpecifications(String tenant, String baseSpecKey, Collection<I> specifications) {
        Optional.ofNullable(specifications)
            .orElseGet(List::of)
            .stream()
            .filter(Objects::nonNull)
            .forEach(f -> processDataSpecification(tenant, baseSpecKey, f));
        definitionSpecProcessor.processDefinitionsItSelf(tenant, baseSpecKey);
        return specifications;
    }

    @Override
    public void updateByTenantState(String tenant, String baseSpecKey, Collection<S> specifications) {
        List<S> filtered = Optional.ofNullable(specifications)
            .orElseGet(List::of)
            .stream()
            .filter(Objects::nonNull)
            .toList();
        filtered.forEach(spec -> updateByTenantState(tenant, baseSpecKey, spec));
        filtered.forEach(spec -> processSpecification(tenant, baseSpecKey, spec));
    }

    public abstract <I extends SpecificationItem> void processDataSpecification(String tenant, String baseSpecKey, I specWithInputData);
    public abstract void updateByTenantState(String tenant, String baseSpecKey, S specification);
}
