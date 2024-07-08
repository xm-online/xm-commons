package com.icthh.xm.commons.flow.domain.flow;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class Action extends Step {
    private Boolean isIterable;
    private String iterableJsonPath;
    private Boolean skipIterableJsonPathError;
    private List<String> next;
}
