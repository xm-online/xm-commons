package com.icthh.xm.commons.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.icthh.xm.commons.config.client.api.refreshable.Specification;
import org.apache.commons.lang3.StringUtils;

public interface HasOutputDataSpec extends Specification {

    @JsonIgnore
    default String getOutputDataSpec() {
        return StringUtils.EMPTY;
    }

    @JsonIgnore
    default void setOutputDataSpec(String spec) {}
}
