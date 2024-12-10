package com.icthh.xm.commons.domain.spec;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.icthh.xm.commons.domain.SpecWithInputDataAndForm;
import com.icthh.xm.commons.domain.enums.FunctionTxTypes;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@link FunctionSpec} class.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"key", "path", "inputSpec", "inputForm", "wrapResult", "anonymous", "txType",
    "tags", "httpMethods"})
@Data
public class FunctionSpec implements IFunctionSpec, SpecWithInputDataAndForm {

    /**
     * Unique in tenant function key.
     */
    @JsonProperty("key")
    private String key;

    /**
     * Unique in tenant HTTP path template.
     * May contain placeholders for path parameters.
     * Example: my/rest/service/{id}
     */
    @JsonProperty("path")
    private String path;

    /**
     * Function input context specification (json-schema for {@code functionInput} arg, see FunctionExecutorService).
     */
    @JsonProperty("inputSpec")
    private String inputSpec;

    /**
     * Form layout for function input context specification (Formly layout for FunctionInput.data).
     */
    @JsonProperty("inputForm")
    private String inputForm;

    /**
     * Return FunctionResult if true, otherwise return real function data (only data)
     */
    @JsonProperty("wrapResult")
    private boolean wrapResult;

    /**
     * Is function accessible by anonymous user
     */
    @JsonProperty("anonymous")
    private Boolean anonymous;

    /**
     * Transaction type for function execution
     */
    @JsonProperty("txType")
    private FunctionTxTypes txType = FunctionTxTypes.TX;

    /**
     * Function api tags
     */
    @JsonProperty("tags")
    private List<String> tags = new ArrayList<>();

    /**
     * Function api http methods
     */
    @JsonProperty("httpMethods")
    private List<String> httpMethods = new ArrayList<>();

    /**
     * Parameter to allow function input data validation
     */
    @JsonProperty("validateFunctionInput")
    private Boolean validateFunctionInput;

    public Boolean getAnonymous() {
        return anonymous != null && anonymous;
    }

    @Override
    public Boolean getWrapResult() {
        return this.wrapResult;
    }

    @Override
    public String getInputDataSpec() {
        return this.getInputSpec();
    }

    @Override
    public void setInputDataSpec(String spec) {
        this.setInputSpec(spec);
    }

    @Override
    public String getInputFormSpec() {
        return this.getInputForm();
    }

    @Override
    public void setInputFormSpec(String spec) {
        this.setInputForm(spec);
    }
}
