package com.icthh.xm.commons.flow.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class Action extends Step {
    private Boolean isIterable;
    private String iterableJsonPath;
    private Boolean skipIterableJsonPathError;
    private Boolean removeNullOutputForIterableResult;
    private String next;

    @Override
    public String getNext(Object context) {
        return next;
    }
}
