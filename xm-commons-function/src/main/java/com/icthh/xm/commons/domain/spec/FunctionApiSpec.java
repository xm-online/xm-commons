package com.icthh.xm.commons.domain.spec;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.icthh.xm.commons.domain.enums.FunctionTxTypes;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@link FunctionApiSpec} class.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"key", "path", "inputSpec", "inputForm", "wrapResult", "onlyData", "anonymous", "txType",
    "tags", "httpMethods"})
@Data
@EqualsAndHashCode(callSuper = true)
public class FunctionApiSpec extends IFunctionSpec {

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
     * Get real function result data if true, or else get binary data
     */
    @JsonProperty("onlyData")
    private Boolean onlyData;

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
     * Parameter to allow dynamic permission checking when function executed
     */
    private boolean dynamicPermissionCheckEnabled;

    /**
     * Parameter to allow function input data validation
     */
    private boolean validateFunctionInput;

    public Boolean getOnlyData() {
        return onlyData == null || onlyData;
    }

    public Boolean getAnonymous() {
        return anonymous != null && anonymous;
    }
}
