package com.icthh.xm.commons.processor.impl;

import com.icthh.xm.commons.domain.DefinitionSpec;
import com.icthh.xm.commons.listener.JsonListenerService;
import com.icthh.xm.commons.processor.DataSpecProcessor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static com.icthh.xm.commons.utils.DataSpecConstants.XM_DEFINITION;
import static java.util.Optional.ofNullable;

@Slf4j
@Service("definitionSpecProcessor")
public class DefinitionSpecProcessor extends DataSpecProcessor<DefinitionSpec> {

    // tenantName -> specKey -> definitionKey -> DefinitionsSpec
    private final Map<String, Map<String, Map<String, DefinitionSpec>>> definitionsByTenant;
    private final Map<String, Map<String, Map<String, DefinitionSpec>>> processedDefinitionsByTenant = new ConcurrentHashMap<>();

    public DefinitionSpecProcessor(JsonListenerService jsonListenerService) {
        super(jsonListenerService);
        this.definitionsByTenant = new ConcurrentHashMap<>();
    }

    @Override
    public String getSectionName() {
        return XM_DEFINITION;
    }

    @Override
    public String getReferencePattern() {
        return "#/" + getSectionName() + "/**/*";
    }

    @Override
    public String getKeyTemplate() {
        return "#/" + getSectionName() + "/{" + KEY + "}/**";
    }

    @Override
    public void updateStateByTenant(String tenant, String dataSpecKey, Collection<DefinitionSpec> definitionSpecs) {
        Map<String, DefinitionSpec> addedDefinitions = toKeyMapOverrideDuplicates(definitionSpecs);
        if (!addedDefinitions.isEmpty()) {
            log.info("added {} definition specs to tenant: {}", addedDefinitions.size(), tenant);
            definitionsByTenant
                .computeIfAbsent(tenant, s -> new HashMap<>())
                .computeIfAbsent(dataSpecKey, s -> new HashMap<>())
                .putAll(addedDefinitions);
        }
    }

    @Override
    public void processDataSpecReferences(String tenant, String dataSpecKey, String spec, Map<String, Map<String, Object>> tenantDataSpecs) {
        findDataSpecReferencesByPattern(spec, getReferencePattern()).forEach(ref ->
            processDefinition(
                tenant, dataSpecKey, tenantDataSpecs, ref,
                definitionsByTenant,
                definitionSpec -> getDefinitionSpecificationByFile(tenant, definitionSpec)
            )
        );
    }

    /**
     * Method to get a list of copy of fully processed definition
     * @param tenant    tenant key name
     * @return          processed definitions copy
     */
    public Collection<DefinitionSpec> getProcessedSpecsCopy(String tenant, String dataSpecKey) {
        return List.copyOf(processedDefinitionsByTenant.getOrDefault(tenant, Map.of()).getOrDefault(dataSpecKey, Map.of()).values());
    }

    public String getDefinitionSpecificationByFile(String tenant, DefinitionSpec definitionSpec) {
        return ofNullable(definitionSpec.getValue())
            .orElseGet(() -> jsonListenerService.getSpecificationByTenantRelativePath(tenant, definitionSpec.getRef()));
    }

    /**
     * This method is used for processing inner definitions
     * @param tenant    tenant key name
     */
    public void processDefinitionsItSelf(String tenant, String dataSpecKey) {
        Map<String, DefinitionSpec> processed = new LinkedHashMap<>();

        definitionsByTenant.getOrDefault(tenant, Map.of()).getOrDefault(dataSpecKey, Map.of())
            .forEach((key, value) ->
                Optional.ofNullable(getDefinitionSpecificationByFile(tenant, value))
                    .filter(StringUtils::isNotBlank)
                    .map(file -> processInnerDataSpec(tenant, dataSpecKey, value.getKey(), file))
                    .ifPresent(spec -> processed.put(key, spec))
            );
        processedDefinitionsByTenant.put(tenant, Map.of(dataSpecKey, Map.copyOf(processed)));
    }

    private DefinitionSpec processInnerDataSpec(String tenant, String dataSpecKey, String specKey, String definitionSpecFile) {
        Mutable<String> definition = new MutableObject<>(definitionSpecFile);
        processDataSpec(tenant, dataSpecKey, definition::setValue, definition::getValue);
        DefinitionSpec spec = new DefinitionSpec();
        spec.setKey(specKey);
        spec.setValue(definition.getValue());
        return spec;
    }
}
