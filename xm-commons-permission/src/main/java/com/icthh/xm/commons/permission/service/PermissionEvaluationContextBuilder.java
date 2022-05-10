package com.icthh.xm.commons.permission.service;

import java.util.Map;
import org.springframework.expression.EvaluationContext;

public interface PermissionEvaluationContextBuilder {

    EvaluationContext build(Map<String, Object> variables);
}
