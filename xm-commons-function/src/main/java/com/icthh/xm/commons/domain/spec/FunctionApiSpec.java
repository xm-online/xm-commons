package com.icthh.xm.commons.domain.spec;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.icthh.xm.commons.domain.enums.FunctionTxTypes;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@link FunctionApiSpec} class.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"key", "path", "inputSpec", "inputForm", "wrapResult", "onlyData", "anonymous", "txType",
    "tags", "httpMethods"})
@Data
public class FunctionApiSpec {

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
     * todo
     */
    @JsonProperty("onlyData")
    private Boolean onlyData;

    /**
     * todo
     */
    @JsonProperty("anonymous")
    private Boolean anonymous;

    /**
     * todo
     */
    @JsonProperty("txType")
    private FunctionTxTypes txType = FunctionTxTypes.TX;

    /**
     * todo
     */
    @JsonProperty("tags")
    private List<String> tags = new ArrayList<>();

    /**
     * todo
     */
    @JsonProperty("httpMethods")
    private List<String> httpMethods = new ArrayList<>();

    /**
     * todo
     */
    private boolean dynamicPermissionCheckEnabled;

    /**
     * todo
     */
    private boolean validateFunctionInput;

    public Boolean getOnlyData() {
        return onlyData == null || onlyData;
    }

    public Boolean getAnonymous() {
        return anonymous != null && anonymous;
    }
}
