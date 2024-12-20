package com.icthh.xm.commons.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Collection;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class TestBaseSpecification implements BaseSpecification {

    @JsonProperty("test-items")
    private Collection<TestSpecificationItem> items;

    @JsonProperty("definitions")
    private List<DefinitionSpec> definitions = null;

    @JsonProperty("forms")
    private List<FormSpec> forms = null;

}
