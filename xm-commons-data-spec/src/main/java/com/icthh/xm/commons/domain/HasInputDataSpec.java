package com.icthh.xm.commons.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.icthh.xm.commons.config.client.api.refreshable.Specification;
import org.apache.commons.lang3.StringUtils;

public interface HasInputDataSpec extends Specification {

    @JsonIgnore
    default String getInputDataSpec() {
        return StringUtils.EMPTY;
    }

    @JsonIgnore
    default void setInputDataSpec(String spec) {}
}
