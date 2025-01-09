package com.icthh.xm.commons.service.impl;

import com.icthh.xm.commons.domain.DefinitionSpec;
import com.icthh.xm.commons.processor.impl.DefinitionSpecProcessor;
import com.icthh.xm.commons.service.FunctionSpecService;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.web.rest.response.DataSchemaResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.icthh.xm.commons.utils.Constants.FUNCTIONS;

@Service
@RequiredArgsConstructor
public class FunctionSpecServiceImpl implements FunctionSpecService {

    public static final String DEFINITION_TYPE = "definition";

    private final TenantContextHolder tenantContextHolder;
    private final DefinitionSpecProcessor definitionSpecProcessor;

    @Override
    public List<DataSchemaResponse> getDataSpecSchemas() {
        return Optional.ofNullable(getProcessedSpecsCopy()).orElse(List.of())
            .stream()
            .map(it -> new DataSchemaResponse(it.getKey(), it.getValue(), DEFINITION_TYPE))
            .collect(Collectors.toList());
    }

    private Collection<DefinitionSpec> getProcessedSpecsCopy() {
        String tenantKey = TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder);
        return definitionSpecProcessor.getProcessedSpecsCopy(tenantKey, FUNCTIONS);
    }
}
