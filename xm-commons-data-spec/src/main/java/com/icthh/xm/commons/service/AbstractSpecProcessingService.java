package com.icthh.xm.commons.service;

import com.icthh.xm.commons.domain.BaseSpecification;
import com.icthh.xm.commons.domain.SpecificationItem;
import com.icthh.xm.commons.processor.impl.DefinitionSpecProcessor;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
public abstract class AbstractSpecProcessingService<S extends BaseSpecification> implements SpecificationProcessingService<S> {

    private final DefinitionSpecProcessor definitionSpecProcessor;

    @Override
    public <I extends SpecificationItem> Collection<I> processDataSpecification(String tenant, String dataSpecKey, Collection<I> specifications) {
        specifications.stream()
            .filter(Objects::nonNull)
            .forEach(f -> processDataSpecification(tenant, dataSpecKey, f));
        definitionSpecProcessor.processDefinitionsItSelf(tenant, dataSpecKey);
        return specifications;
    }

    @Override
    public void updateByTenantState(String tenant, String dataSpecKey, Collection<S> specifications) {
        List<S> filtered = specifications.stream().filter(Objects::nonNull).toList();
        filtered.forEach(spec -> updateByTenantState(tenant, dataSpecKey, spec));
        filtered.forEach(spec -> processSpecification(tenant, dataSpecKey, spec));
    }

    public abstract <I extends SpecificationItem> void processDataSpecification(String tenant, String dataSpecKey, I specWithInputData);
    public abstract void updateByTenantState(String tenant, String dataSpecKey, S specification);
}
