package com.icthh.xm.commons.config;

import com.icthh.xm.commons.domain.spec.FunctionApiSpecs;
import com.icthh.xm.commons.domain.spec.FunctionSpec;
import com.icthh.xm.commons.processor.impl.DefinitionSpecProcessor;
import com.icthh.xm.commons.processor.impl.FormSpecProcessor;
import com.icthh.xm.commons.service.DefaultSpecProcessingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FunctionApiSpecsProcessor extends DefaultSpecProcessingService<FunctionApiSpecs> {

    private final DefinitionSpecProcessor definitionSpecProcessor;
    private final FormSpecProcessor formSpecProcessor;

    public FunctionApiSpecsProcessor(DefinitionSpecProcessor definitionSpecProcessor,
                                     FormSpecProcessor formSpecProcessor) {
        super(definitionSpecProcessor, formSpecProcessor);
        this.definitionSpecProcessor = definitionSpecProcessor;
        this.formSpecProcessor = formSpecProcessor;
    }

    @Override
    public FunctionApiSpecs processSpecification(String tenant, String dataSpecKey, FunctionApiSpecs specification) {
        specification.getSpecifications().forEach(f -> {
            processValidateInputParameter(specification.isValidateFunctionInput(), f);
            definitionSpecProcessor.processDataSpec(tenant, dataSpecKey, f::setInputSpec, f::getInputSpec);
            formSpecProcessor.processDataSpec(tenant, dataSpecKey, f::setInputForm, f::getInputForm);
        });
        definitionSpecProcessor.processDefinitionsItSelf(tenant, dataSpecKey);
        return specification;
    }

    private void processValidateInputParameter(boolean isValidateFunctionInput, FunctionSpec functionSpec) {
        if (functionSpec.getValidateFunctionInput() == null) {
            functionSpec.setValidateFunctionInput(isValidateFunctionInput);
        }
    }
}