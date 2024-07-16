package com.icthh.xm.commons.flow.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Condition extends Step {
    private String nextOnConditionTrue;
    private String nextOnConditionFalse;

    @Override
    public String getNext(Object context) {
        return Boolean.TRUE.equals(context) ? nextOnConditionTrue : nextOnConditionFalse;
    }
}
