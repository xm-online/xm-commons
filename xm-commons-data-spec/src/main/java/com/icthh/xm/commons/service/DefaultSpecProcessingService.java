package com.icthh.xm.commons.service;

import com.icthh.xm.commons.domain.BaseSpecification;
import com.icthh.xm.commons.domain.SpecificationItem;
import com.icthh.xm.commons.processor.impl.DefinitionSpecProcessor;
import com.icthh.xm.commons.processor.impl.FormSpecProcessor;
import org.springframework.stereotype.Service;

@Service
public class DefaultSpecProcessingService<S extends BaseSpecification> extends AbstractSpecProcessingService<S> {

    private final DefinitionSpecProcessor definitionSpecProcessor;
    private final FormSpecProcessor formSpecProcessor;

    public DefaultSpecProcessingService(DefinitionSpecProcessor definitionSpecProcessor,
                                        FormSpecProcessor formSpecProcessor) {
        super(definitionSpecProcessor);
        this.definitionSpecProcessor = definitionSpecProcessor;
        this.formSpecProcessor = formSpecProcessor;
    }

    @Override
    public <I extends SpecificationItem> void processDataSpecification(String tenant, String baseSpecKey, I spec) {
        definitionSpecProcessor.processDataSpec(tenant, baseSpecKey, spec::setInputDataSpec, spec::getInputDataSpec);
        formSpecProcessor.processDataSpec(tenant, baseSpecKey, spec::setInputFormSpec, spec::getInputFormSpec);
    }

    @Override
    public void updateByTenantState(String tenant, String baseSpecKey, S spec) {
        definitionSpecProcessor.updateStateByTenant(tenant, baseSpecKey, spec.getDefinitions());
        formSpecProcessor.updateStateByTenant(tenant, baseSpecKey, spec.getForms());
    }
}
