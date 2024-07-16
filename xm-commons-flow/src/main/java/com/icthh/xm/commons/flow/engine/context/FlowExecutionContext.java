package com.icthh.xm.commons.flow.engine.context;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class FlowExecutionContext {

    private final String flowKey;
    private final Object input;
    private Object output;
    private final Map<String, Object> stepInput = new HashMap<>();
    private final Map<String, Object> stepOutput = new HashMap<>();

    private Integer iteration;
    private Object iterationItem;
    private List<Object> iterationsInput;
    private List<Object> iterationsOutput;

    public void resetIteration() {
        iteration = null;
        iterationItem = null;
        iterationsInput = new ArrayList<>();
        iterationsOutput = new ArrayList<>();
    }
}
