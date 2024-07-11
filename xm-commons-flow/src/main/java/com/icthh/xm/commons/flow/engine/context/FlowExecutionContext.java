package com.icthh.xm.commons.flow.engine.context;

import lombok.Data;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class FlowExecutionContext {

    private Object input;
    private final Map<String, Object> stepInput = new ConcurrentHashMap<>();
    private final Map<String, Object> stepOutput = new ConcurrentHashMap<>();

}
