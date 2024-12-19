package com.icthh.xm.commons.domain;

import com.icthh.xm.commons.config.client.api.refreshable.Specification;
import org.apache.commons.lang3.StringUtils;

public interface HasValue extends Specification {

    default String getValue() {
        return StringUtils.EMPTY;
    }

    default void setValue(String value) {}
}
