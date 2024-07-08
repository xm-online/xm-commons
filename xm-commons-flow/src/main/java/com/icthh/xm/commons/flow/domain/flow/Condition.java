package com.icthh.xm.commons.flow.domain.flow;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class Condition extends Step {
    private List<String> nextOnConditionTrue;
    private List<String> nextOnConditionFalse;
}
