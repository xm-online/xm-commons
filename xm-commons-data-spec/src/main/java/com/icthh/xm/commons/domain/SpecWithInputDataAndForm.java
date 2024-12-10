package com.icthh.xm.commons.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.StringUtils;

public interface SpecWithInputDataAndForm {

    @JsonIgnore
    default String getInputDataSpec() {
        return StringUtils.EMPTY;
    }

    @JsonIgnore
    default void setInputDataSpec(String spec) {}

    @JsonIgnore
    default String getInputFormSpec() {
        return StringUtils.EMPTY;
    }

    @JsonIgnore
    default void setInputFormSpec(String spec) {}
}
