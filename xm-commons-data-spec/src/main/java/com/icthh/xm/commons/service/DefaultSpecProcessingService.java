package com.icthh.xm.commons.service;

import com.icthh.xm.commons.domain.BaseSpecification;
import com.icthh.xm.commons.domain.HasInputDataForm;
import com.icthh.xm.commons.domain.HasInputDataSpec;
import com.icthh.xm.commons.domain.HasOutputDataForm;
import com.icthh.xm.commons.domain.HasOutputDataSpec;
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
        if (spec instanceof HasInputDataSpec s) {
            definitionSpecProcessor.processDataSpec(tenant, baseSpecKey, s::setInputDataSpec, s::getInputDataSpec);
        }
        if (spec instanceof HasOutputDataSpec s) {
            definitionSpecProcessor.processDataSpec(tenant, baseSpecKey, s::setOutputDataSpec, s::getOutputDataSpec);
        }
        if (spec instanceof HasInputDataForm s) {
            formSpecProcessor.processDataSpec(tenant, baseSpecKey, s::setInputFormSpec, s::getInputFormSpec);
        }
        if (spec instanceof HasOutputDataForm s) {
            formSpecProcessor.processDataSpec(tenant, baseSpecKey, s::setOutputFormSpec, s::getOutputFormSpec);
        }
    }

    @Override
    public void updateByTenantState(String tenant, String baseSpecKey, S spec) {
        definitionSpecProcessor.updateStateByTenant(tenant, baseSpecKey, spec.getDefinitions());
        formSpecProcessor.updateStateByTenant(tenant, baseSpecKey, spec.getForms());
    }
}
