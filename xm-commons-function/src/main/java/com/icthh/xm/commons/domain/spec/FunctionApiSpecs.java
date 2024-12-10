package com.icthh.xm.commons.domain.spec;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.icthh.xm.commons.domain.DefinitionSpec;
import com.icthh.xm.commons.domain.FormSpec;
import com.icthh.xm.commons.domain.SpecWithDefinitionAndForm;
import lombok.Data;

import java.util.Collection;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"validateFunctionInput", "functions", "definitions", "forms"})
@Data
public class FunctionApiSpecs implements SpecWithDefinitionAndForm {

    /**
     * Collection of service spi function specifications
     */
    @JsonProperty("functions")
    private Collection<FunctionSpec> functions;

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

    /**
     * Parameter to allow function input data validation
     */
    @JsonProperty("validateFunctionInput")
    private boolean validateFunctionInput;

    @Override
    public Collection<FunctionSpec> getSpecifications() {
        return this.functions;
    }
}
