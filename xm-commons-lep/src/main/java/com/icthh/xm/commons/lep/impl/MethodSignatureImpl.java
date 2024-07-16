package com.icthh.xm.commons.lep.impl;

import com.icthh.xm.commons.lep.api.BaseLepContext;
import com.icthh.xm.commons.lep.api.UseAsLepContext;
import com.icthh.xm.lep.api.MethodSignature;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;

public class MethodSignatureImpl implements MethodSignature {

    private static final String[] STRINGS_EMPTY_ARRAY = new String[0];

    private final Class<?> declaringClass;
    private final List<String> parameterNames;
    private final Map<String, Integer> parameterIndexes;
    private final String[] parameterNamesArray;
    private final Method method;
    private final String declaringClassName;
    private final String lepContextMethodParameter;

    public MethodSignatureImpl(Method method, Class<?> targetType) {
        this.method = method;
        this.lepContextMethodParameter = calculateLepContextMethodParameter(method);
        this.parameterNames = calculateParametersNames(method);
        this.parameterIndexes = calculateParametersIndexes(this.parameterNames);
        this.parameterNamesArray = this.parameterNames.toArray(STRINGS_EMPTY_ARRAY);
        this.declaringClass = targetType;
        this.declaringClassName = (declaringClass != null) ? declaringClass.getName() : null;
    }

    private String calculateLepContextMethodParameter(Method method) {
        Parameter[] parameters = method.getParameters();
        if (parameters != null) {
            return stream(parameters)
                .filter(p -> BaseLepContext.class.isAssignableFrom(p.getType()))
                .filter(p -> p.getAnnotation(UseAsLepContext.class) != null)
                .findAny()
                .map(Parameter::getName)
                .orElse(null);
        }
        return null;
    }

    private Map<String, Integer> calculateParametersIndexes(List<String> parameterNames) {
        Map<String, Integer> parameterIndexes = new HashMap<>();
        for (int i = 0; i < parameterNames.size(); i++) {
            parameterIndexes.put(parameterNames.get(i), i);
        }
        return unmodifiableMap(parameterIndexes);
    }

    private List<String> calculateParametersNames(Method method) {
        Parameter[] parameters = method.getParameters();
        if (parameters == null) {
            return emptyList();
        } else {
            List<String> parameterNames = new ArrayList<>(parameters.length);
            for (Parameter p: parameters) {
                parameterNames.add(p.getName());
            }
            return unmodifiableList(parameterNames);
        }
    }

    @Override
    public String getName() {
        return method.getName();
    }

    @Override
    public int getModifiers() {
        return method.getModifiers();
    }

    @Override
    public Class<?> getDeclaringClass() {
        return declaringClass;
    }

    @Override
    public String getDeclaringClassName() {
        return this.declaringClassName;
    }

    @Override
    public Class<?>[] getParameterTypes() {
        return method.getParameterTypes();
    }

    @Override
    public String[] getParameterNames() {
        return parameterNamesArray;
    }

    @Override
    public List<String> getParameterNamesList() {
        return parameterNames;
    }

    @Override
    public Integer getParameterIndex(String name) {
        return parameterIndexes.get(name);
    }

    @Override
    public Class<?>[] getExceptionTypes() {
        return method.getExceptionTypes();
    }

    @Override
    public Class<?> getReturnType() {
        return method.getReturnType();
    }

    @Override
    public Method getMethod() {
        return method;
    }

    @Override
    public String getLepContextMethodParameter() {
        return lepContextMethodParameter;
    }

}
