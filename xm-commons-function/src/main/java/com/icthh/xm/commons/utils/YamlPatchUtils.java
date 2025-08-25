package com.icthh.xm.commons.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.exceptions.EntityNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.api.lowlevel.Compose;
import org.snakeyaml.engine.v2.exceptions.Mark;
import org.snakeyaml.engine.v2.nodes.MappingNode;
import org.snakeyaml.engine.v2.nodes.Node;
import org.snakeyaml.engine.v2.nodes.NodeTuple;
import org.snakeyaml.engine.v2.nodes.ScalarNode;
import org.snakeyaml.engine.v2.nodes.SequenceNode;

@Slf4j
public class YamlPatchUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

    public static String addObject(String yamlText, Object value, List<YamlPatchPattern> path) {
        var rootNode = loadYamlNode(yamlText);
        if (rootNode.isEmpty()) {
            log.warn("Failed to load YAML from text: {}", yamlText);
            return yamlText;
        }

        List<YamlNode> result = searchNodes(new YamlNode(rootNode.get(), null), path, 0);
        if (result.isEmpty()) {
            log.warn("No nodes found for path: {}", printPath(path));
            return yamlText;
        }

        YamlNode targetNode = result.getFirst();
        List<String> lines = yamlText.lines().toList();
        int line = targetNode.node.getEndMark().map(Mark::getLine).orElse(lines.size());
        return insertString(value, targetNode, yamlText, line);
    }

    public static String addSequenceItem(String yamlText, Object value, List<YamlPatchPattern> path) {
        return addObject(yamlText, List.of(value), path);
    }

    public static String delete(String yamlText, List<YamlPatchPattern> path) {
        var rootNode = loadYamlNode(yamlText);
        if (rootNode.isEmpty()) {
            log.warn("Failed to load YAML from text: {}", yamlText);
            return yamlText;
        }

        List<YamlNode> result = searchNodes(new YamlNode(rootNode.get(), null), path, 0);
        List<Range> toDelete = result.stream()
            .map(YamlPatchUtils::getLinesRange)
            .filter(Objects::nonNull)
            .toList();

        return removeRangesFromString(yamlText, toDelete);
    }

    public static String updateSequenceItem(String yamlText, Object value, List<YamlPatchPattern> path) {
        return updateObject(yamlText, List.of(value), path);
    }

    public static String updateObject(String yamlText, Object value, List<YamlPatchPattern> path) {
        var rootNode = loadYamlNode(yamlText);
        if (rootNode.isEmpty()) {
            log.warn("Failed to load YAML from text: {}", yamlText);
            return yamlText;
        }

        List<YamlNode> result = searchNodes(new YamlNode(rootNode.get(), null), path, 0);
        if (result.isEmpty()) {
            log.warn("No nodes found for path: {}", printPath(path));
            throw new EntityNotFoundException("Nodes not found for path: " + printPath(path));
        }

        YamlNode targetNode = result.getFirst();
        Range toUpdate = getLinesRange(targetNode);
        String updateText = removeRangesFromString(yamlText, List.of(toUpdate));
        return insertString(value, targetNode.parent, updateText, toUpdate.startLine - 1);
    }

    public static YamlPatchPattern key(String key) {
        return wrapper(node -> {
            List<Node> result = new ArrayList<>();
            if (node instanceof MappingNode mappingNode) {
                for (NodeTuple t : mappingNode.getValue()) {
                    Node keyNode = t.getKeyNode();
                    if (key.equals(keyToString(keyNode))) {
                        result.add(t.getValueNode());
                    }
                }
            }
            return result;
        }, "." + key);
    }

    public static YamlPatchPattern array() {
        return wrapper(node -> {
            List<Node> result = new ArrayList<>();
            if (node instanceof SequenceNode sequenceNode) {
                result.addAll(sequenceNode.getValue());
            }
            return result;
        }, "[]");
    }

    public static YamlPatchPattern arrayByField(Map<String, String> fieldValues) {
        return wrapper(node -> {
            List<Node> result = new ArrayList<>();
            if (node instanceof SequenceNode sequenceNode) {
                for (Node item: sequenceNode.getValue()) {
                    if (item instanceof MappingNode mappingNode) {
                        List<NodeTuple> tuple = mappingNode.getValue();
                        Map<String, Object> values = convertTupleToMap(tuple);
                        if (compareAllFields(fieldValues, values)) {
                            result.add(item);
                        }
                    }
                }
            }
            return result;
        }, "[" + fieldValues.entrySet().stream()
            .map(entry -> entry.getKey() + "=" + entry.getValue())
            .reduce((a, b) -> a + ", " + b).orElse("") + "]");
    }

    private static String insertString(Object value, YamlNode targetNode, String yamlText, int line) {
        List<String> lines = yamlText.lines().toList();
        int startingSpaces = getStartingSpaces(targetNode);
        String valueYaml = buildTargetYaml(value, startingSpaces);
        StringBuilder resultYaml = new StringBuilder();
        for(int i = 0; i < lines.size(); i++) {
            resultYaml.append(lines.get(i)).append("\n");
            if (i == line) {
                resultYaml.append(valueYaml);
            }
        }
        if (lines.size() <= line) {
            resultYaml.append(valueYaml);
        }
        return resultYaml.toString();
    }

    private static String printPath(List<YamlPatchPattern> path) {
        return String.join("", path.stream().map(YamlPatchPattern::toString).toList());
    }

    @SneakyThrows
    private static String buildTargetYaml(Object value, int startingSpaces) {
        String valueYaml = objectMapper.writeValueAsString(value);
        Stream<String> lines = valueYaml.lines();
        StringBuilder valueString = new StringBuilder();
        lines.skip(1).forEach(line -> {
            if (StringUtils.isBlank(line)) {
                valueString.append("\n");
            } else {
                valueString.append(" ".repeat(startingSpaces)).append(line).append("\n");
            }
        });
        return valueString.toString();
    }

    private static int getStartingSpaces(YamlNode yamlNode) {
        int defaultSpaces = yamlNode.parent == null ? 2 : yamlNode.parent.node.getStartMark().map(Mark::getColumn).orElse(0) + 2;
        switch (yamlNode.node) {
            case MappingNode mappingNode -> {
                List<NodeTuple> values = mappingNode.getValue();
                if (values.isEmpty()) {
                    return defaultSpaces;
                }
                return values.getFirst().getKeyNode().getStartMark().map(Mark::getColumn).orElse(defaultSpaces);
            }
            case SequenceNode sequenceNode -> {
                List<Node> values = sequenceNode.getValue();
                if (values.isEmpty()) {
                    return defaultSpaces;
                }
                Node firstItem = values.getFirst();
                return firstItem.getStartMark()
                    .map(Mark::getColumn)
                    .map(it -> it - 2) // strip started "- " prefix
                    .orElse(defaultSpaces);
            }
            default -> {
                return defaultSpaces;
            }
        }
    }

    private static Optional<Node> loadYamlNode(String yamlText) {
        LoadSettings settings = LoadSettings.builder().build();
        Compose compose = new Compose(settings);
        return compose.composeString(yamlText);
    }

    private static String removeRangesFromString(String yamlText, List<Range> toDelete) {
        StringBuilder sb = new StringBuilder();
        List<String> lines = yamlText.lines().toList();
        for (int i = 0; i < lines.size(); i++) {
            int line = i;
            boolean isDeleted = toDelete.stream().anyMatch(range -> range.isInside(line));
            if (!isDeleted) {
                sb.append(lines.get(i)).append("\n");
            }
        }
        return sb.toString();
    }

    public static List<YamlNode> searchNodes(YamlNode yamlNode, List<YamlPatchPattern> path, int pathIndex) {
        if (pathIndex >= path.size()) {
            return List.of(yamlNode);
        }
        List<Node> acceptedChildren = path.get(pathIndex).apply(yamlNode.node);
        return acceptedChildren.stream().flatMap(it -> searchNodes(new YamlNode(it, yamlNode), path, pathIndex + 1).stream())
            .filter(Objects::nonNull)
            .toList();
    }

    private static Range getLinesRange(YamlNode yamlNode) {
        Optional<Mark> startMarkOpt = yamlNode.node.getStartMark();
        Optional<Mark> endMarkOpt = yamlNode.node.getEndMark();
        if (startMarkOpt.isEmpty() || endMarkOpt.isEmpty()) {
            return null;
        }
        return new Range(startMarkOpt.get().getLine(), endMarkOpt.get().getLine());
    }

    private static String keyToString(Node keyNode) {
        if (keyNode instanceof ScalarNode s) return s.getValue();
        return "[complex-key]";
    }

    private static Map<String, Object> convertTupleToMap(List<NodeTuple> tuple) {
        Map<String, Object> values = new HashMap<>();
        for (NodeTuple t: tuple) {
            Node keyNode = t.getKeyNode();
            String key = keyToString(keyNode);
            if (t.getValueNode() instanceof ScalarNode scalarNode) {
                values.put(key, scalarNode.getValue());
            }
        }
        return values;
    }

    private static boolean compareAllFields(Map<String, String> fieldValues, Map<String, Object> values) {
        return fieldValues.entrySet().stream().allMatch(entry -> Objects.equals(entry.getValue(), values.get(entry.getKey())));
    }

    public static abstract class YamlPatchPattern {
        abstract List<Node> apply(Node node);
    }

    public static YamlPatchPattern wrapper(Function<Node, List<Node>> function, String presentation) {
        return new YamlPatchPattern() {
            @Override
            List<Node> apply(Node node) {
                return function.apply(node);
            }

            @Override
            public String toString() {
                return presentation;
            }
        };
    }

    public record Range(int startLine, int endLine) {
        public boolean isInside(int line) {
            return line >= startLine && line < endLine;
        }
    }

    public record YamlNode(Node node, YamlNode parent){}

}
