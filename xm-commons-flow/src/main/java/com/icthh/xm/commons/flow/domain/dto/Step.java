package com.icthh.xm.commons.flow.domain.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.icthh.xm.commons.flow.spec.step.StepSpec;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = Action.class, name = "ACTION"),
    @JsonSubTypes.Type(value = Condition.class, name = "CONDITION")
})
public class Step {
    private String key;
    private String typeKey;
    private List<String> depends;
    private Map<String, Object> parameters;
    private Map<String, Snippet> snippets;
    private StepSpec.StepType type;

    @Data
    public static class Snippet {
        private String content;
        private String extension;
    }
}
