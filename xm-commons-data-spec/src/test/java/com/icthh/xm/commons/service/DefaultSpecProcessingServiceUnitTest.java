package com.icthh.xm.commons.service;

import com.icthh.xm.commons.domain.BaseSpecification;
import com.icthh.xm.commons.domain.BaseSpecificationItem;
import com.icthh.xm.commons.domain.DefinitionSpec;
import com.icthh.xm.commons.domain.FormSpec;
import com.icthh.xm.commons.domain.TestBaseSpecification;
import com.icthh.xm.commons.domain.TestSpecificationItem;
import com.icthh.xm.commons.processor.impl.DefinitionSpecProcessor;
import com.icthh.xm.commons.processor.impl.FormSpecProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.icthh.xm.commons.utils.TestConstants.BASE_SPEC_KEY;
import static com.icthh.xm.commons.utils.TestConstants.TEST_TENANT;
import static com.icthh.xm.commons.utils.TestReadSpecUtils.loadBaseSpecByFileName;
import static org.apache.commons.collections.ListUtils.union;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class DefaultSpecProcessingServiceUnitTest {

    @Mock
    private DefinitionSpecProcessor definitionSpecProcessor;

    @Mock
    private FormSpecProcessor formSpecProcessor;

    private DefaultSpecProcessingService<BaseSpecification> processingService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        processingService = new DefaultSpecProcessingService<>(definitionSpecProcessor, formSpecProcessor);
    }

    @Test
    void processDataSpecification_withBaseSpecificationItem() {
        TestBaseSpecification spec = loadBaseSpecByFileName("test-spec-simple");
        BaseSpecificationItem specItem = spec.getItems().iterator().next();

        processingService.processDataSpecification(TEST_TENANT, BASE_SPEC_KEY, specItem);

        verify(definitionSpecProcessor, times(2)).processDataSpec(eq(TEST_TENANT), eq(BASE_SPEC_KEY), any(), any());
        verify(formSpecProcessor, times(2)).processDataSpec(eq(TEST_TENANT), eq(BASE_SPEC_KEY), any(), any());
    }

    @Test
    void updateByTenantState_filterNullSpecifications() {
        TestBaseSpecification specWithEmptyItems = new TestBaseSpecification();
        TestBaseSpecification spec = loadBaseSpecByFileName("test-spec-simple");
        int itemsNumber = spec.getItems().size();

        spec.getItems().add(null);

        List<DefinitionSpec> definitions = union(spec.getDefinitions(), Optional.ofNullable(specWithEmptyItems.getDefinitions()).orElseGet(List::of));
        List<FormSpec> formSpecs = union(spec.getForms(), Optional.ofNullable(specWithEmptyItems.getForms()).orElseGet(List::of));

        processingService.updateByTenantState(TEST_TENANT, BASE_SPEC_KEY, Set.of(spec, specWithEmptyItems));

        verify(definitionSpecProcessor).fullUpdateStateByTenant(TEST_TENANT, BASE_SPEC_KEY, definitions);
        verify(formSpecProcessor).fullUpdateStateByTenant(TEST_TENANT, BASE_SPEC_KEY, formSpecs);

        verify(definitionSpecProcessor, times(itemsNumber * 2)).processDataSpec(eq(TEST_TENANT), eq(BASE_SPEC_KEY), any(), any());
        verify(formSpecProcessor, times(itemsNumber * 2)).processDataSpec(eq(TEST_TENANT), eq(BASE_SPEC_KEY), any(), any());
    }

    @Test
    void processSpecification_filterNullSpecifications() {
        processingService.processSpecification(TEST_TENANT, BASE_SPEC_KEY, new TestBaseSpecification());

        verify(definitionSpecProcessor).processDefinitionsItSelf(TEST_TENANT, BASE_SPEC_KEY);

        verifyNoMoreInteractions(definitionSpecProcessor);
        verifyNoMoreInteractions(formSpecProcessor);
    }

    @Test
    void processSpecification_withEmptySpecifications() {
        TestBaseSpecification spec = new TestBaseSpecification();
        Collection<TestSpecificationItem> items = new ArrayList<>();
        items.add(null);
        spec.setItems(items);

        processingService.processSpecification(TEST_TENANT, BASE_SPEC_KEY, spec);

        verify(definitionSpecProcessor).processDefinitionsItSelf(TEST_TENANT, BASE_SPEC_KEY);

        verifyNoMoreInteractions(definitionSpecProcessor);
        verifyNoMoreInteractions(formSpecProcessor);
    }

    @Test
    void processSpecification_withNonEmptySpecifications() {
        TestBaseSpecification spec = loadBaseSpecByFileName("test-spec-simple");
        int itemsNumber = spec.getItems().size();

        processingService.processSpecification(TEST_TENANT, BASE_SPEC_KEY, spec);

        verify(definitionSpecProcessor, times(itemsNumber * 2)).processDataSpec(eq(TEST_TENANT), eq(BASE_SPEC_KEY), any(), any());
        verify(formSpecProcessor, times(itemsNumber * 2)).processDataSpec(eq(TEST_TENANT), eq(BASE_SPEC_KEY), any(), any());

        verify(definitionSpecProcessor).processDefinitionsItSelf(TEST_TENANT, BASE_SPEC_KEY);
    }
}
