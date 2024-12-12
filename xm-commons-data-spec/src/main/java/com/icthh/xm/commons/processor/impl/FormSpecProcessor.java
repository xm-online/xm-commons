package com.icthh.xm.commons.processor.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.icthh.xm.commons.domain.FormSpec;
import com.icthh.xm.commons.listener.JsonListenerService;
import com.icthh.xm.commons.processor.SpecProcessor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.collect.Lists.newArrayList;
import static com.icthh.xm.commons.utils.DataSpecConstants.PROCESSING_ITERATION_LIMIT;
import static com.icthh.xm.commons.utils.DataSpecConstants.XM_FORM;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Slf4j
@Service("formSpecProcessor")
public class FormSpecProcessor extends SpecProcessor<FormSpec> {

    // tenantName -> specKey -> definitionKey -> DefinitionsSpec
    private final Map<String, Map<String, Map<String, FormSpec>>> formsByTenant;

    public FormSpecProcessor(JsonListenerService jsonListenerService) {
        super(jsonListenerService);
        this.formsByTenant = new ConcurrentHashMap<>();
    }

    @Override
    public String getSectionName() {
        return XM_FORM;
    }

    @Override
    public String getReferencePattern() {
        return "#/" + getSectionName() + "/**/*";
    }

    @Override
    public String getKeyTemplate() {
        return getKeyTemplatePrefix() + "{" + FORM_KEY + "}";
    }

    private String getKeyTemplatePrefix() {
        return "#/" + getSectionName() + "/";
    }

    @Override
    public void updateStateByTenant(String tenant, String dataSpecKey, Collection<FormSpec> formSpecs) {
        Map<String, FormSpec> addedForms = toKeyMapOverrideDuplicates(formSpecs);
        if (!addedForms.isEmpty()) {
            log.info("added {} form specs to tenant: {}", addedForms.size(), tenant);
            formsByTenant
                .computeIfAbsent(tenant, s -> new HashMap<>())
                .computeIfAbsent(dataSpecKey, s -> new HashMap<>())
                .putAll(addedForms);
        }
    }

    @Override
    public void processDataSpec(String tenant, String dataSpecKey, Consumer<String> setter, Supplier<String> getter) {
        String typeSpecForm = getter.get();
        if (isBlank(typeSpecForm) || formsByTenant.getOrDefault(tenant, Map.of()).isEmpty()) {
            return;
        }
        String updatedSpecForm = typeSpecForm;

        for (int i = 0; i < PROCESSING_ITERATION_LIMIT; i++) {
            Set<String> existingReferences = findDataSpecReferencesByPattern(updatedSpecForm, getReferencePattern());

            if (existingReferences.isEmpty()) {
                setter.accept(updatedSpecForm);
                return;
            }
            Map<String, String> specifications = collectTenantSpecifications(existingReferences, tenant, dataSpecKey);
            updatedSpecForm = resolveReferences(specifications, updatedSpecForm);
        }
        log.warn("Max iteration limit reached: {}. Skip current form processing", PROCESSING_ITERATION_LIMIT);
    }

    private Map<String, String> collectTenantSpecifications(Set<String> existingReferences, String tenant, String dataSpecKey) {
        Map<String, String> specificationsByRelativePath = new LinkedHashMap<>();

        for (String formPath : existingReferences) {
            String formKey = matcher.extractUriTemplateVariables(getKeyTemplate(), formPath).get(FORM_KEY);
            if (formKey.isBlank()) {
                continue;
            }
            ofNullable(formsByTenant.getOrDefault(tenant, Map.of()).get(dataSpecKey))
                .map(formMap -> formMap.get(formKey))
                .map(formSpec -> getFormSpecificationByFile(tenant, formSpec))
                .filter(StringUtils::isNotBlank)
                .ifPresentOrElse(
                    specification -> specificationsByRelativePath.put(getKeyTemplatePrefix() + formKey, specification),
                    () -> log.warn("The form specification for key:{} and tenant:{} was not found.", formKey, tenant));
        }
        return specificationsByRelativePath;
    }

    private String getFormSpecificationByFile(String tenant, FormSpec formSpec) {
        return ofNullable(formSpec.getValue())
            .orElseGet(() -> jsonListenerService.getSpecificationByTenantRelativePath(tenant, formSpec.getRef()));
    }

    @SneakyThrows
    private String resolveReferences(Map<String, String> specifications, String formSpec) {
        JsonNode defaults = jsonMapper.readValue(formSpec, JsonNode.class);
        JsonNode jsonNode = replaceReferences(defaults, specifications, null);
        return jsonMapper.writeValueAsString(jsonNode);
    }

    private JsonNode replaceReferences(JsonNode node, Map<String, String> specifications, ContainerNode<?> parentNode) {
        if (node.isObject()) {
            replaceReferencesFromObject(node, specifications, parentNode);
        } else if (node.isArray()) {
            replaceReferencesFromArray(node, specifications, parentNode);
        }
        return node;
    }

    private JsonNode replaceReferencesFromObject(JsonNode node, Map<String, String> specifications, ContainerNode<?> parentNode) {
        ObjectNode objectNode = (ObjectNode) node;

        Map<String, JsonNode> objectMap = copyToMap(objectNode.fields());
        JsonNode refNode = objectMap.get(REF);
        JsonNode keyNode = objectMap.getOrDefault(KEY, ymlMapper.createObjectNode());

        objectNode.remove(REF);
        if (refNode != null && objectNode.size() == 1) {
            objectNode.remove(KEY);
        }

        String refPath = firstNonNull(refNode, ymlMapper.createObjectNode()).asText();
        String formSpec = specifications.getOrDefault(refPath, "{}");
        var fromSpecNode = this.convertSpecificationToObjectNodes(formSpec, refPath);
        processFromSpecNode(fromSpecNode, keyNode.asText());
        injectJsonNode(parentNode, objectNode, fromSpecNode, refPath);

        objectMap.values().forEach(childNode -> replaceReferences(childNode, specifications, objectNode));
        return node;
    }

    private JsonNode replaceReferencesFromArray(JsonNode node, Map<String, String> specifications, ContainerNode<?> parentNode) {
        List<JsonNode> copiedList = newArrayList(node.iterator());
        for (JsonNode jsonNode : copiedList) {
            replaceReferences(jsonNode, specifications, (ArrayNode) node);
        }
        return node;
    }

    private Map<String, JsonNode> copyToMap(Iterator<Map.Entry<String, JsonNode>> iterator) {
        Map<String, JsonNode> jsonNodeMap = new LinkedHashMap<>();
        iterator.forEachRemaining((entry) -> jsonNodeMap.put(entry.getKey(), entry.getValue()));
        return jsonNodeMap;
    }

    @SneakyThrows
    private ContainerNode<?> convertSpecificationToObjectNodes(String specification, String ref) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(specification, ContainerNode.class);
        } catch (JsonProcessingException e) {
            log.error("The form specification by ref: {} could not be processed. Error: {}", ref, e.getMessage());
            throw e;
        }
    }

    private void processFromSpecNode(JsonNode fromSpecNode, String prefix) {
        if (isBlank(prefix)) {
            return;
        }
        if (fromSpecNode.isObject()) {
            ObjectNode objectNode = (ObjectNode) fromSpecNode;
            Map<String, JsonNode> objectMap = copyToMap(objectNode.fields());
            JsonNode keyNode = objectMap.get(KEY);
            if (keyNode != null) {
                objectNode.put(KEY, prefix + '.' + keyNode.asText());
            } else if (objectMap.containsKey(REF)) {
                objectNode.put(KEY, prefix);
            }
            objectMap.values().forEach(it -> this.processFromSpecNode(it, prefix));
        } else if (fromSpecNode.isArray()) {
            List<JsonNode> copiedList = newArrayList(fromSpecNode.iterator());
            for (JsonNode jsonNode : copiedList) {
                processFromSpecNode(jsonNode, prefix);
            }
        }
    }

    private void injectJsonNode(ContainerNode<?> parentNode, ObjectNode objectNode, ContainerNode<?> jsonNode, String refPath) {
        if (jsonNode.isArray() && parentNode.isObject()) {
            log.warn("Array by $ref {} has been injected to the object instead of array.", refPath);
        } else if (jsonNode.isArray() && parentNode.isArray()) {
            processArrayNode((ArrayNode) jsonNode, (ArrayNode) parentNode, objectNode);
        } else {
            objectNode.setAll((ObjectNode) jsonNode);
            removeEmptyObject(parentNode, objectNode);
        }
    }

    private void processArrayNode(ArrayNode arrayNode, ArrayNode parentArray, ObjectNode objectNode) {
        List<JsonNode> arrayNodesList = newArrayList(arrayNode.iterator());
        List<JsonNode> newParentArray = newArrayList(parentArray.elements());
        int index = indexOfByReference(objectNode, newParentArray);
        newParentArray.addAll(index + 1, arrayNodesList);
        if (objectNode.isEmpty()) {
            newParentArray.remove(objectNode);
        }
        parentArray.removeAll();
        parentArray.addAll(newParentArray);
    }

    private static int indexOfByReference(ObjectNode objectNode, List<JsonNode> list) {
        return IntStream.range(0, list.size())
            .filter(i -> objectNode == list.get(i))
            .findFirst()
            .orElse(-1);
    }

    private static void removeEmptyObject(ContainerNode<?> parentNode, ObjectNode objectNode) {
        if (parentNode != null && parentNode.isArray() && objectNode.isEmpty()) {
            List<JsonNode> elements = newArrayList(parentNode.elements());
            elements.remove(indexOfByReference(objectNode, elements));
            parentNode.removeAll();
            ((ArrayNode) parentNode).addAll(elements);
        }
    }
}
