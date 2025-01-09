package com.icthh.xm.commons.swagger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.icthh.xm.commons.exceptions.BusinessException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.logging.log4j.util.TriConsumer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;
import static com.icthh.xm.commons.utils.DataSpecConstants.DEFINITIONS;
import static com.icthh.xm.commons.utils.DataSpecConstants.XM_DEFINITION;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Slf4j
public class JsonSchemaToSwaggerSchemaConverter {

    private static final Set<String> supportedKeywords = Set.of(
        "$ref", DEFINITIONS,

        // Keywords with the same meaning as in JSON Schema
        "title", "pattern", "required", "enum", "minimum", "maximum",
        "exclusiveMinimum", "exclusiveMaximum", "multipleOf", "minLength",
        "maxLength", "minItems", "maxItems", "uniqueItems", "minProperties",
        "maxProperties",

        // Keywords with minor differences
        "type", "format", "description", "items", "properties",
        "additionalProperties", "default", "allOf", "oneOf",
        "anyOf", "not",

        // Additional keywords
        "deprecated", "discriminator", "example", "externalDocs",
        "nullable", "readOnly", "writeOnly", "xml"
    );

    private static final Set<String> validTypes = Set.of(
        "null", "boolean", "object", "array", "number", "string", "integer"
    );
    public static final String COMPONENTS_SCHEMAS = "#/components/schemas/";
    public final ObjectMapper objectMapper;
    private final String definitionSectionName;
    private final Set<String> definitionPrefixes;

    public JsonSchemaToSwaggerSchemaConverter() {
        this(XM_DEFINITION, Set.of(DEFINITIONS, XM_DEFINITION));
    }

    public JsonSchemaToSwaggerSchemaConverter(String definitionSectionName, Set<String> definitionPrefixes) {
        this.objectMapper = new ObjectMapper();
        this.definitionSectionName = definitionSectionName;
        this.definitionPrefixes = definitionPrefixes;
    }

    public String transformToSwaggerJson(String typeName, String jsonSchema,
                                         Map<String, Object> definitions,
                                         Map<String, Object> originalDefinitions) {
        JsonNode json = transformToJsonNode(typeName, jsonSchema, definitions, originalDefinitions);
        return writeJson(json);
    }

    public JsonNode transformToJsonNode(String typeName, String jsonSchema,
                                        Map<String, Object> definitions,
                                        Map<String, Object> originalDefinitions) {
        if (isBlank(jsonSchema)) {
            return instance.nullNode();
        }

        JsonNode inputJson = readJson(jsonSchema);
        if (!inputJson.isObject()) {
            throw new IllegalArgumentException("Json schema should be an object");
        }

        ObjectNode json = (ObjectNode) inputJson;
        if (!json.has("type") && !json.has("properties") && !json.has("$ref")) {
            removeEmptyDefinition(json);
            ObjectNode object = object("type", instance.textNode("object"));
            object.set("properties", json);
            json = object;
        }
        if (json.has("properties") && !json.has("type")) {
            removeEmptyDefinition(json);
            json.put("type", "object");
        }
        transformToSwaggerJson(typeName, json, definitions, originalDefinitions, instance.objectNode());
        return json;
    }

    private void removeEmptyDefinition(ObjectNode json) {
        if (json.has(definitionSectionName) && json.get(definitionSectionName).isObject() && json.get(definitionSectionName).isEmpty()) {
            json.remove(definitionSectionName);
        }
    }

    private void transformToSwaggerJson(String typeName, ObjectNode json,
                                        Map<String, Object> definitions,
                                        Map<String, Object> originalDefinitions, ObjectNode objectDefinitions) {
        if (!json.isObject()) {
            return;
        }

        processDefinitions(typeName, json, definitions, originalDefinitions, objectDefinitions);
        traverseSchema(instance.nullNode(), typeName, json, this::convertElement);
        traverseSchema(instance.nullNode(), typeName, json, this::rewriteUnsupportedKeywords);
    }

    private void traverseSchema(JsonNode parent, String fieldName, JsonNode json, TriConsumer<JsonNode, String, ObjectNode> convertElement) {
        if (json == null) {
            return;
        }

        if (json.isArray()) {
            int index = 0;
            json.forEach(it -> {
                traverseSchema(json, fieldName + "__" + index, it, convertElement);
            });
        } else if (json.isObject()) {
            convert(parent, fieldName, json, convertElement);
            json.fields().forEachRemaining(it -> {
                traverseSchema(json, it.getKey(), it.getValue(), convertElement);
            });
        }
    }

    private void convert(JsonNode parent, String fieldName, JsonNode json, TriConsumer<JsonNode, String, ObjectNode> function) {
        if (json == null || !json.isObject()) {
            return;
        }
        function.accept(parent, fieldName, (ObjectNode) json);
    }

    private void convertElement(JsonNode parent, String fieldName, ObjectNode object) {
        fixArrayDeclarations(object);
        convertNullable(parent, fieldName, object);
        rewriteConst(object);
        rewriteIfThenElse(object);
        rewriteExclusiveMinMax(object);
        convertDependencies(parent, fieldName, object);
        removeEmptyRequired(object);
    }

    private void fixArrayDeclarations(ObjectNode object) {
        if (object.has("type") && object.get("type").asText().equals("array")
            && object.has("items") && object.get("items").isArray() && object.get("items").size() == 1) {
            object.set("items", object.get("items").get(0));
        }
    }

    private void removeEmptyRequired(ObjectNode object) {
        if (object.has("required") && object.get("required").isArray() && object.get("required").isEmpty()) {
            object.remove("required");
        }
    }

    private void convertDependencies(JsonNode parent, String fieldName, ObjectNode schema) {
        if (insideProperties(parent, fieldName)) {
            return;
        }

        JsonNode deps = schema.get("dependencies");
        if (deps == null || !deps.isObject()) {
            return;
        }

        schema.remove("dependencies");

        ArrayNode allOf;
        if (schema.has("allOf") && schema.get("allOf").isArray()) {
            allOf = (ArrayNode) schema.get("allOf");
        } else {
            allOf = schema.putArray("allOf");
        }

        deps.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            JsonNode value = entry.getValue();

            List<JsonNode> requiredArray = new ArrayList<>();
            requiredArray.add(schema.textNode(key));
            if (value.isArray()) {
                for (JsonNode item : value) {
                    requiredArray.add(item);
                }
            } else {
                requiredArray.add(value);
            }
            JsonNode allOfItem = object("oneOf", array(List.of(
                object("not", object(
                    "required", array(List.of(schema.textNode(key)))
                )),
                object("required", array(requiredArray))
            )));

            allOf.add(allOfItem);
        });
    }

    private void rewriteUnsupportedKeywords(JsonNode parent, String parentFieldName, ObjectNode json) {
        if (insideProperties(parent, parentFieldName)) {
            return;
        }
        List<String> toRewrite = new ArrayList<>();
        Iterator<String> fieldNames = json.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            if (!supportedKeywords.contains(fieldName) && !fieldName.startsWith("x-")) {
                toRewrite.add(fieldName);
            }
        }
        toRewrite.forEach(fieldName -> json.set("x-" + fieldName, json.remove(fieldName)));
    }

    private boolean insideProperties(JsonNode parent, String parentFieldName) {
        return parentFieldName.equals("properties") && parent.has("type") && parent.get("type").asText().equals("object");
    }

    private void processDefinitions(String typeName, ObjectNode json,
                                    Map<String, Object> definitions,
                                    Map<String, Object> originalDefinitions,
                                    ObjectNode objectDefinitions) {
        for (var definitionsField: definitionPrefixes) {
            if (json.has(definitionsField)) {
                objectDefinitions.set(definitionsField, json.remove(definitionsField));
            }
        }

        traverseSchema(instance.nullNode(), typeName, json, (parent, fieldName, object) -> {
            if (object.has("$ref")) {
                String ref = object.get("$ref").asText();
                Optional<String> definitionField = definitionPrefixes.stream().map(it -> "#/" + it + "/").filter(ref::startsWith).findFirst();
                if (definitionField.isEmpty()) {
                    return;
                }
                String typePath = ref.substring((definitionField.get()).length());
                JsonNode currentNode = readByPath(ref, objectDefinitions);
                String definitionName = getDefinitionName(typeName, originalDefinitions, typePath, currentNode);
                if (currentNode != null && currentNode.isObject()) {
                    ObjectNode definitionNode = currentNode.deepCopy();
                    definitions.put(definitionName, definitionNode);
                    originalDefinitions.put(definitionName, definitionNode.deepCopy());
                    object.put("$ref", COMPONENTS_SCHEMAS + definitionName);
                    transformToSwaggerJson(definitionName, definitionNode, definitions, originalDefinitions, objectDefinitions);
                }
            }
        });
    }

    private JsonNode readByPath(String ref, ObjectNode objectNode) {
        String path = ref.replace("#/", "");
        JsonNode currentNode = objectNode;
        for (var field: path.split("/")) {
            if (currentNode.has(field)) {
                currentNode = currentNode.get(field);
            } else {
                log.warn("Definition not found: {}", ref);
                currentNode = null;
                break;
            }
        }
        return currentNode;
    }

    private String getDefinitionName(String typeName, Map<String, Object> definitions, String path, JsonNode currentNode) {
        String[] segments = path.split("/");
        String uniqKey = "";
        for (int i = segments.length - 1; i >= 0; i--) {
            uniqKey = capitalize(segments[i]) + uniqKey;
            if (!containsDefinition(definitions, currentNode, uniqKey)) {
                return uniqKey;
            }
        }

        if (!containsDefinition(definitions, currentNode, uniqKey)) {
            return uniqKey;
        }

        uniqKey = capitalize(typeName) + uniqKey;
        int i = 1;
        String tempKey = uniqKey;
        while (containsDefinition(definitions, currentNode, uniqKey)) {
            tempKey = uniqKey + i;
            i++;
        }

        return tempKey;
    }

    private static boolean containsDefinition(Map<String, Object> definitions, JsonNode currentNode, String uniqKey) {
        return definitions.get(uniqKey) != null && !definitions.get(uniqKey).equals(currentNode);
    }

    private void validateTypes(JsonNode parent, String fieldName, JsonNode typeBlock) {
        JsonNode type = typeBlock.get("type");
        if (type.has("properties") || type.has("type") || type.has("anyOf") || type.has("oneOf")) {
            return;
        }
        if (insideProperties(parent, fieldName)) {
            return;
        }
        if (type.has("$ref")) {
            String ref = type.get("$ref").asText();
            if (definitionPrefixes.stream().map(it -> "#/" + it + "/").noneMatch(ref::startsWith)) {
                throw new BusinessException("error.invalid.json.type.ref", "Invalid ref: " + ref,
                    Map.of("json", typeBlock.toString(), "fieldName", fieldName));
            }
            return;
        }
        if (type.isArray()) {
            type.forEach(it -> {
                if (!validTypes.contains(it.asText())) {
                    throw new BusinessException("error.invalid.json.type", "Invalid type: " + it.asText(),
                        Map.of("json", typeBlock.toString(), "fieldName", fieldName));
                }
            });
            return;
        }
        if (!validTypes.contains(type.asText())) {
            throw new BusinessException("error.invalid.json.type", "Invalid type: " + type.asText(),
                Map.of("json", typeBlock.toString(), "fieldName", fieldName));
        }
    }

    private Object convertNullable(JsonNode parent, String fieldName, ObjectNode json) {
        JsonNode type = json.get("type");
        if (type == null) {
            return json;
        }

        validateTypes(parent, fieldName, json);

        if (type.isNull() || type.asText().equals("null")) {
            json.remove("type");
            json.put("nullable", true);
            return json;
        }

        if (type.asText().equals("array") && !json.has("items")) {
            json.putObject("items");
        }

        if (type.isArray()) {
            ArrayNode typeArrayNode = (ArrayNode) type;
            Iterator<JsonNode> elements = typeArrayNode.elements();
            while (elements.hasNext()) {
                var it = elements.next();
                if (it.isNull() || it.asText().equals("null")) {
                    elements.remove();
                    json.put("nullable", true);
                }
            }

            if (typeArrayNode.isEmpty()) {
                json.remove("type");
            } else if (typeArrayNode.size() == 1) {
                json.set("type", typeArrayNode.get(0));
            } else if (typeArrayNode.size() > 1) {
                List<JsonNode> items = IteratorUtils.toList(typeArrayNode.elements());
                var types = items.stream().map(it -> object("type", it)).collect(toList());
                json.set("anyOf", array(types));
                json.remove("type");
            }
        }

        return json;
    }

    private void rewriteConst(ObjectNode jsonNode) {
        if (jsonNode.has("const") && jsonNode.get("const").isValueNode()) {
            JsonNode value = jsonNode.get("const");
            if (value.isNumber()) {
                jsonNode.put("type", "number");
            } else if (value.isTextual()) {
                jsonNode.put("type", "string");
            } else if (value.isBoolean()) {
                jsonNode.put("type", "boolean");
            }
            jsonNode.set("enum", array(List.of(value)));
            jsonNode.remove("const");
        }
    }

    private void rewriteIfThenElse(ObjectNode json) {
        if (json.has("if") && json.has("than")) {
            json.set("oneOf", array(
                List.of(
                    object("allOf", array(List.of(json.get("if"), json.get("then")))),
                    object("allOf", array(List.of(json.get("if"), json.get("else"))))
                )
            ));
        }
    }

    private void rewriteExclusiveMinMax(ObjectNode json) {
        if (json.has("type") && (
            json.get("type").asText().equals("number") || json.get("type").asText().equals("integer")
        )) {
            if (json.has("exclusiveMinimum") && json.get("exclusiveMinimum").isNumber()) {
                json.set("minimum", json.get("exclusiveMinimum"));
                json.put("exclusiveMinimum", true);
            }
            if (json.has("exclusiveMaximum") && json.get("exclusiveMaximum").isNumber()) {
                json.set("maximum", json.get("exclusiveMaximum"));
                json.put("exclusiveMaximum", true);
            }
        }
    }

    private static ArrayNode array(List<? extends JsonNode> items) {
        items = items.stream().filter(Objects::nonNull).collect(toList());
        return instance.arrayNode().addAll(items);
    }

    private static ObjectNode object(String type, JsonNode it) {
        ObjectNode objectNode = instance.objectNode();
        objectNode.set(type, it);
        return objectNode;
    }

    @SneakyThrows
    private String writeJson(Object jsonSchema) {
        return objectMapper.writeValueAsString(jsonSchema);
    }

    @SneakyThrows
    private JsonNode readJson(String jsonSchema) {
        return objectMapper.readTree(jsonSchema);
    }

    public void inlineRootRef(JsonNode jsonNode, Map<String, Object> definitions) {
        if (!(jsonNode instanceof ObjectNode)) {
            return;
        }

        if (jsonNode.has("$ref")) {
            inlineRef(jsonNode.get("$ref").asText(), (ObjectNode) jsonNode, definitions);
        } else if (jsonNode.has("properties") && jsonNode.get("properties").has("$ref")) {
            inlineRef(jsonNode.get("properties").get("$ref").asText(), (ObjectNode) jsonNode.get("properties"), definitions);
        }
    }

    private void inlineRef(String ref, ObjectNode jsonNode, Map<String, Object> definitions) {
        if (!ref.startsWith(COMPONENTS_SCHEMAS)) {
            return;
        }
        ref = ref.substring(COMPONENTS_SCHEMAS.length());

        var refNode = definitions.get(ref);
        if (!(refNode instanceof ObjectNode)) {
            return;
        }

        var refObject = (ObjectNode) refNode;
        refObject.fields().forEachRemaining(entry -> {
            jsonNode.set(entry.getKey(), entry.getValue().deepCopy());
        });
    }
}
