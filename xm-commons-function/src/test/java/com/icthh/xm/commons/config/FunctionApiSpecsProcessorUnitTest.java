package com.icthh.xm.commons.config;

import com.icthh.xm.commons.domain.spec.FunctionApiSpecs;
import com.icthh.xm.commons.domain.spec.FunctionSpec;
import com.icthh.xm.commons.processor.impl.DefinitionSpecProcessor;
import com.icthh.xm.commons.processor.impl.FormSpecProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static com.icthh.xm.commons.utils.FunctionSpecReaderUtils.loadFunctionApiSpecByFile;
import static com.icthh.xm.commons.utils.FunctionUtils.findFunctionSpecByKey;
import static com.icthh.xm.commons.utils.TestConstants.BASE_SPEC_KEY;
import static com.icthh.xm.commons.utils.TestConstants.TENANT_KEY;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class FunctionApiSpecsProcessorUnitTest {

    @Mock
    private DefinitionSpecProcessor definitionSpecProcessor;

    @Mock
    private FormSpecProcessor formSpecProcessor;

    @InjectMocks
    private FunctionApiSpecsProcessor processor;

    private FunctionApiSpecs spec1;
    private FunctionApiSpecs spec2;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        spec1 = loadFunctionApiSpecByFile("functions");
        spec2 = loadFunctionApiSpecByFile("functions-anonymous");
    }

    @Test
    void processSpecification_processEachFunctionSpec() {
        processor.processSpecification(TENANT_KEY, BASE_SPEC_KEY, spec1);

        verify(definitionSpecProcessor, times(4)).processDataSpec(eq(TENANT_KEY), eq(BASE_SPEC_KEY), any(), any());
        verify(formSpecProcessor, times(4)).processDataSpec(eq(TENANT_KEY), eq(BASE_SPEC_KEY), any(), any());

        verify(definitionSpecProcessor).processDefinitionsItSelf(TENANT_KEY, BASE_SPEC_KEY);
    }

    @Test
    void processSpecification_setGlobalValidateFunctionInputWhenNull() {
        FunctionApiSpecs result1 = processor.processSpecification(TENANT_KEY, BASE_SPEC_KEY, spec1);

        assertTrue(spec1.isValidateFunctionInput());
        assertFalse(findFunctionSpecByKey(result1, "store/STORE-INFO").getValidateFunctionInput());
        assertTrue(findFunctionSpecByKey(result1, "store/GET-EMPLOYEES-AGE").getValidateFunctionInput());

        FunctionApiSpecs result2 = processor.processSpecification(TENANT_KEY, BASE_SPEC_KEY, spec2);

        assertFalse(spec2.isValidateFunctionInput()); // default value
        assertFalse(findFunctionSpecByKey(result2, "item/SEARCH-ITEMS-BY-STORE").getValidateFunctionInput());
        assertTrue(findFunctionSpecByKey(result2, "item/SEARCH-ITEMS-BY-CATEGORY").getValidateFunctionInput());
    }

    @Test
    void processSpecification_skipNullItems() {
        spec1.setItems(null);

        processor.processSpecification(TENANT_KEY, BASE_SPEC_KEY, spec1);

        verify(definitionSpecProcessor).processDefinitionsItSelf(TENANT_KEY, BASE_SPEC_KEY);

        verifyNoMoreInteractions(definitionSpecProcessor, formSpecProcessor);
    }

    @Test
    void processSpecification_handleNullItems() {
        List<FunctionSpec> items = new ArrayList<>();
        items.add(null);
        items.add(spec1.getItems().iterator().next());
        items.add(null);
        items.add(null);
        spec1.setItems(items);

        processor.processSpecification(TENANT_KEY, BASE_SPEC_KEY, spec1);

        verify(definitionSpecProcessor, times(2)).processDataSpec(eq(TENANT_KEY), eq(BASE_SPEC_KEY), any(), any());
        verify(formSpecProcessor, times(2)).processDataSpec(eq(TENANT_KEY), eq(BASE_SPEC_KEY), any(), any());
        verify(definitionSpecProcessor).processDefinitionsItSelf(TENANT_KEY, BASE_SPEC_KEY);

        verifyNoMoreInteractions(definitionSpecProcessor, formSpecProcessor);
    }
}
