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
    @JsonSubTypes.Type(value = ActionDto.class, name = "ACTION"),
    @JsonSubTypes.Type(value = ConditionDto.class, name = "CONDITION")
})
public class StepDto {
    private String key;
    private String typeKey;
    private List<String> depends;
    private Map<String, Object> parameters;
    private Map<String, String> jsSnippets;
    private StepSpec.StepType type;
}
