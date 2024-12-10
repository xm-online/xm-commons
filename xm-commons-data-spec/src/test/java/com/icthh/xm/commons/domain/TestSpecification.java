package com.icthh.xm.commons.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Collection;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class TestSpecification implements SpecWithDefinitionAndForm {

    /**
     * Collection of service spi function specifications
     */
    @JsonProperty("functions")
    private Collection<TestInputDataSpec> specifications;

    /**
     * Collection of data specification definitions
     */
    @JsonProperty("definitions")
    private List<DefinitionSpec> definitions = null;

    /**
     * Collection data specification forms
     */
    @JsonProperty("forms")
    private List<FormSpec> forms = null;

}
