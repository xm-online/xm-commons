package com.icthh.xm.commons.domain;

import com.icthh.xm.commons.config.client.api.refreshable.Specification;
import org.apache.commons.lang3.StringUtils;

public interface HasRef extends Specification {

    default String getRef() {
        return StringUtils.EMPTY;
    }

    default void setRef(String ref) {}
}
