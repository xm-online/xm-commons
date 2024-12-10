package com.icthh.xm.commons.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"key", "ref", "value"})
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class DefinitionSpec implements DataSpec {

    @JsonProperty("key")
    private String key;

    @JsonProperty("ref")
    private String ref;

    @JsonProperty("value")
    private String value;
}
