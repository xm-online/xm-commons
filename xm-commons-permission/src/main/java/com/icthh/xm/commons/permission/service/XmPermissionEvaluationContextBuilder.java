package com.icthh.xm.commons.permission.service;

import com.icthh.xm.commons.permission.utils.RequestHeaderUtils;
import java.lang.reflect.Method;
import java.util.Map;
import lombok.SneakyThrows;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;

public class XmPermissionEvaluationContextBuilder implements PermissionEvaluationContextBuilder {

    private static final Method GET_REQUEST_HEADER = lookupGetRequestHeaderMethod();

    @Override
    public EvaluationContext build(Map<String, Object> variables) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setVariables(variables);
        registerFunctions(context);
        return context;
    }

    protected void registerFunctions(StandardEvaluationContext context) {
        context.registerFunction("getRequestHeader", GET_REQUEST_HEADER);
    }

    @SneakyThrows
    private static Method lookupGetRequestHeaderMethod() {
        return RequestHeaderUtils.class.getDeclaredMethod("getRequestHeader", String.class);
    }
}
