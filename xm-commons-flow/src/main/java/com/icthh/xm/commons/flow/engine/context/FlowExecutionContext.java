package com.icthh.xm.commons.flow.engine.context;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class FlowExecutionContext {

    private final String flowKey;
    private Object input;
    private Object output;
    private final Map<String, Object> stepInput = new HashMap<>();
    private final Map<String, Object> stepOutput = new HashMap<>();

}
