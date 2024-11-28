package com.icthh.xm.commons.service;

import java.util.Map;

public interface FunctionExecutorService {

    Map<String, Object> execute(String functionKey, Map<String, Object> functionInput, String httpMethod);

    Map<String, Object> executeAnonymousFunction(String functionKey, Map<String, Object> functionInput, String httpMethod);
}
