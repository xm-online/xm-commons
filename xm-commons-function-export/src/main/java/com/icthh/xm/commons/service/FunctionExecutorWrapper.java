package com.icthh.xm.commons.service;

import com.icthh.xm.commons.domain.FunctionResult;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;

public interface FunctionExecutorWrapper {

    FunctionResult execute(String functionKey, String fileFormat, Map<String, Object> functionInput, HttpServletResponse response);
}
