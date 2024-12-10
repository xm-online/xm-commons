package com.icthh.xm.commons.service;

import com.icthh.xm.commons.domain.SpecWithDefinitionAndForm;
import com.icthh.xm.commons.domain.SpecWithInputDataAndForm;
import com.icthh.xm.commons.processor.impl.DefinitionSpecProcessor;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Stream;

@RequiredArgsConstructor
public abstract class AbstractSpecProcessingService<S extends SpecWithDefinitionAndForm> implements SpecificationProcessingService<S> {

    private final DefinitionSpecProcessor definitionSpecProcessor;

    @Override
    public <I extends SpecWithInputDataAndForm> Collection<I> processDataSpecification(String tenant, String dataSpecKey, Collection<I> specifications) {
        specifications.stream()
            .filter(Objects::nonNull)
            .forEach(f -> processDataSpecification(tenant, dataSpecKey, f));
        definitionSpecProcessor.processDefinitionsItSelf(tenant, dataSpecKey);
        return specifications;
    }

    @Override
    public void updateByTenantState(String tenant, String dataSpecKey, Collection<S> specifications) {
        Stream<S> filtered = specifications.stream().filter(Objects::nonNull);
        filtered.forEach(spec -> updateByTenantState(tenant, dataSpecKey, spec));
        filtered.forEach(spec -> processSpecification(tenant, dataSpecKey, spec));
    }

    public abstract <I extends SpecWithInputDataAndForm> void processDataSpecification(String tenant, String dataSpecKey, I specWithInputData);
    public abstract void updateByTenantState(String tenant, String dataSpecKey, S specification);
}
