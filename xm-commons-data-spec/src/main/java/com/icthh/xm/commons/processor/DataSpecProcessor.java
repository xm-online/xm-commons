package com.icthh.xm.commons.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.icthh.xm.commons.domain.DataSpec;
import com.icthh.xm.commons.listener.JsonListenerService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.icthh.xm.commons.ObjectMapperUtils.deserializeToMap;
import static io.micrometer.common.util.StringUtils.isBlank;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
public abstract class DataSpecProcessor<S extends DataSpec> extends SpecProcessor<S> {

    public DataSpecProcessor(JsonListenerService jsonListenerService) {
        super(jsonListenerService);
    }

    public abstract void processDataSpecReferences(String tenant, String dataSpecKey, String spec, Map<String, Map<String, Object>> tenantDataSpecs);

    @Override
    public void processDataSpec(String tenant, String dataSpecKey, Consumer<String> setter, Supplier<String> getter) {
        String dataSpec = getter.get();
        if (isBlank(dataSpec)) {
            log.debug("Skipped empty data spec for tenant: {}", tenant);
            return;
        }
        try {
            Map<String, Object> resultSpec = deserializeToMap(dataSpec);
            // sectionName -> specKey -> spec
            Map<String, Map<String, Object>> tenantDataSpecContententMap = new LinkedHashMap<>();
            processDataSpecReferences(tenant, dataSpecKey, dataSpec, tenantDataSpecContententMap);
            resultSpec.putAll(tenantDataSpecContententMap);

            String mergedJson = jsonMapper.writeValueAsString(resultSpec);
            setter.accept(mergedJson);
        } catch (Exception e) {
            log.error("Could not process data spec by tenant {}: ", tenant, e);
        }
    }

    /**
     * TODO
     * @param tenant
     * @param tenantDataSpecMap
     * @param dataRef
     * @param originsMap
     * @param specMapper
     * @param <T>
     */
    public  <T> void processDefinition(String tenant, String dataSpecKey,
                                       Map<String, Map<String, Object>> tenantDataSpecMap,
                                       String dataRef,
                                       Map<String, Map<String, Map<String, T>>> originsMap,
                                       Function<T, String> specMapper) {
        String keyTemplate = getKeyTemplate();
        String key = matcher.extractUriTemplateVariables(keyTemplate, dataRef).get(KEY);
        Map<String, Object> entityDefinitions = tenantDataSpecMap.computeIfAbsent(getSectionName(), k -> new LinkedHashMap<>());

        if (!entityDefinitions.containsKey(key) && isNotBlank(key)) {
            ofNullable(originsMap.get(tenant))
                .map(t -> t.get(dataSpecKey))
                .map(x -> x.get(key))
                .map(specMapper)
                .filter(StringUtils::isNotBlank)
                .ifPresentOrElse(
                    specification -> {
                        addSpecificationToDefinitionMap(specification, key, entityDefinitions);
                        processDataSpecReferences(tenant, dataSpecKey, specification, tenantDataSpecMap);
                    },
                    () -> log.warn("The specification for key:{} and tenant:{} was not found.",
                        key, tenant));
        }
    }

    private void addSpecificationToDefinitionMap(String specification, String definitionKey,
                                                 Map<String, Object> definitions) {
        try {
            definitions.put(definitionKey, jsonMapper.readValue(specification, Map.class));
        } catch (JsonProcessingException exception) {
            log.warn("Definition specification by key: {} couldn't be parsed: {}", definitionKey, exception.getMessage());
        }
    }

}
