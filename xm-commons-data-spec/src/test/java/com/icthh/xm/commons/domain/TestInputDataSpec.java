package com.icthh.xm.commons.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"key", "path", "inputSpec", "inputForm", "wrapResult", "anonymous", "txType",
    "tags", "httpMethods"})
@Data
public class TestInputDataSpec implements SpecWithInputDataAndForm {

    @JsonProperty("key")
    private String key;

    @JsonProperty("inputSpec")
    private String inputSpec;

    @JsonProperty("inputForm")
    private String inputForm;

    // TODO test, that this field are ignored in json
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
