package com.icthh.xm.commons.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.icthh.xm.commons.config.client.api.refreshable.Specification;
import org.apache.commons.lang3.StringUtils;

public interface HasOutputDataForm extends Specification {

    @JsonIgnore
    default String getOutputFormSpec() {
        return StringUtils.EMPTY;
    }

    @JsonIgnore
    default void setOutputFormSpec(String spec) {}
}
