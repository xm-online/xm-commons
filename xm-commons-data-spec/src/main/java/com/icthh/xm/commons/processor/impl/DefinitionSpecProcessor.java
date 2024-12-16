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
import static java.util.stream.Collectors.toMap;

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

    /**
     *
     * @param tenant            specification tenant
     * @param baseSpecKey       base specification key
     * @param definitionSpecs   data specifications to be updated in storage
     */
    @Override
    public void updateStateByTenant(String tenant, String baseSpecKey, Collection<DefinitionSpec> definitionSpecs) {
        Map<String, DefinitionSpec> addedDefinitions = toKeyMapOverrideDuplicates(definitionSpecs);
        if (!addedDefinitions.isEmpty()) {
            log.info("added {} definition specs to tenant: {}", addedDefinitions.size(), tenant);
            definitionsByTenant
                .computeIfAbsent(tenant, s -> new HashMap<>())
                .computeIfAbsent(baseSpecKey, s -> new HashMap<>())
                .putAll(addedDefinitions);
        }
    }

    @Override
    public void processDataSpecReferences(String tenant, String baseSpecKey, String spec, Map<String, Map<String, Object>> tenantDataSpecs) {
        findDataSpecReferencesByPattern(spec, getReferencePattern()).forEach(ref ->
            processDefinition(
                tenant, baseSpecKey, tenantDataSpecs, ref,
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
    public Collection<DefinitionSpec> getProcessedSpecsCopy(String tenant, String baseSpecKey) {
        return List.copyOf(processedDefinitionsByTenant.getOrDefault(tenant, Map.of()).getOrDefault(baseSpecKey, Map.of()).values());
    }

    public String getDefinitionSpecificationByFile(String tenant, DefinitionSpec definitionSpec) {
        return ofNullable(definitionSpec.getValue())
            .orElseGet(() -> jsonListenerService.getSpecificationByTenantRelativePath(tenant, definitionSpec.getRef()));
    }

    /**
     * This method is used for processing inner definitions
     * @param tenant    tenant key name
     */
    public void processDefinitionsItSelf(String tenant, String baseSpecKey) {
        var processed = definitionsByTenant
            .getOrDefault(tenant, Map.of())
            .getOrDefault(baseSpecKey, Map.of())
            .entrySet()
            .stream()
            .map(e -> Map.entry(e.getKey(), getProcessedInputDataSpec(tenant, baseSpecKey, e.getValue())))
            .filter(e -> e.getValue().isPresent())
            .collect(toMap(Map.Entry::getKey, entry -> entry.getValue().get(), (e1, e2) -> e1, LinkedHashMap::new));

        var byTenant = processedDefinitionsByTenant.computeIfAbsent(tenant, s -> new HashMap<>());
        byTenant.put(baseSpecKey, Map.copyOf(processed));
        processedDefinitionsByTenant.put(tenant, byTenant);
    }

    private Optional<DefinitionSpec> getProcessedInputDataSpec(String tenant, String baseSpecKey, DefinitionSpec specToProcess) {
        return Optional.ofNullable(getDefinitionSpecificationByFile(tenant, specToProcess))
            .filter(StringUtils::isNotBlank)
            .map(file -> processInnerDataSpec(tenant, baseSpecKey, specToProcess.getKey(), file));
    }

    private DefinitionSpec processInnerDataSpec(String tenant, String baseSpecKey, String specKey, String definitionSpecFile) {
        Mutable<String> definition = new MutableObject<>(definitionSpecFile);
        processDataSpec(tenant, baseSpecKey, definition::setValue, definition::getValue);
        DefinitionSpec spec = new DefinitionSpec();
        spec.setKey(specKey);
        spec.setValue(definition.getValue());
        return spec;
    }
}
