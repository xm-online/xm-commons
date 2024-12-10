package com.icthh.xm.commons.service;

import com.icthh.xm.commons.domain.SpecWithInputDataAndForm;
import com.icthh.xm.commons.domain.SpecWithDefinitionAndForm;
import com.icthh.xm.commons.processor.impl.DefinitionSpecProcessor;
import com.icthh.xm.commons.processor.impl.FormSpecProcessor;
import org.springframework.stereotype.Service;

@Service
public class DefaultSpecProcessingService<S extends SpecWithDefinitionAndForm> extends AbstractSpecProcessingService<S> {

    private final DefinitionSpecProcessor definitionSpecProcessor;
    private final FormSpecProcessor formSpecProcessor;

    public DefaultSpecProcessingService(DefinitionSpecProcessor definitionSpecProcessor,
                                        FormSpecProcessor formSpecProcessor) {
        super(definitionSpecProcessor);
        this.definitionSpecProcessor = definitionSpecProcessor;
        this.formSpecProcessor = formSpecProcessor;
    }

    @Override
    public <I extends SpecWithInputDataAndForm> void processDataSpecification(String tenant, String dataSpecKey, I spec) {
        definitionSpecProcessor.processDataSpec(tenant, dataSpecKey, spec::setInputDataSpec, spec::getInputDataSpec);
        formSpecProcessor.processDataSpec(tenant, dataSpecKey, spec::setInputFormSpec, spec::getInputFormSpec);
    }

    @Override
    public void updateByTenantState(String tenant, String dataSpecKey, S spec) {
        definitionSpecProcessor.updateStateByTenant(tenant, dataSpecKey, spec.getDefinitions());
        formSpecProcessor.updateStateByTenant(tenant, dataSpecKey, spec.getForms());
    }
}
