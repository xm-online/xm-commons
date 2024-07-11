package com.icthh.xm.commons.flow.context;

import com.icthh.xm.commons.flow.service.TenantResourceConfigService;
import com.icthh.xm.commons.lep.api.LepAdditionalContext;
import com.icthh.xm.commons.lep.api.LepAdditionalContextField;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.icthh.xm.commons.flow.context.TenantResourceLepAdditionalContext.TenantResourceLepAdditionalContextField.FIELD_NAME;

@Component
@RequiredArgsConstructor
public class TenantResourceLepAdditionalContext implements LepAdditionalContext<Map<String, Map<String, Map<String, Object>>>> {

    private final TenantResourceConfigService resourceService;

    @Override
    public String additionalContextKey() {
        return FIELD_NAME;
    }

    @Override
    public Map<String, Map<String, Map<String, Object>>> additionalContextValue() {
        return resourceService.getResourcesDataByType();
    }

    @Override
    public Class<? extends LepAdditionalContextField> fieldAccessorInterface() {
        return TenantResourceLepAdditionalContextField.class;
    }

    public interface TenantResourceLepAdditionalContextField extends LepAdditionalContextField {
        String FIELD_NAME = "resources";
        default Map<String, Map<String, Map<String, Object>>> getResources() {
            return (Map<String, Map<String, Map<String, Object>>>)get(FIELD_NAME);
        }
    }
}
