package com.icthh.xm.commons.service;

import java.util.Map;

public interface FunctionExecutorService {

    Object execute(String functionKey, Map<String, Object> functionInput, String httpMethod);

    Object executeAnonymousFunction(String functionKey, Map<String, Object> functionInput, String httpMethod);
}
