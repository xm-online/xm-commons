package com.icthh.xm.commons.lep.keyresolver;

import com.icthh.xm.lep.api.LepKeyResolver;
import com.icthh.xm.lep.api.LepMethod;

import java.util.List;

/**
 * The {@link FunctionLepKeyResolver} class.
 */
public class FunctionLepKeyResolver implements LepKeyResolver {

    /**
     * Method parameter name for {@code functionKey}.
     */
    private static final String PARAM_FUNCTION_KEY = "functionKey";

    @Override
    public String group(LepMethod method) {
        String baseGroup = LepKeyResolver.super.group(method);
        String functionKey = method.getParameter(PARAM_FUNCTION_KEY, String.class);
        int groupIndex = functionKey.lastIndexOf('/');
        return (groupIndex < 0) ? baseGroup : baseGroup + "." + functionKey.substring(0, groupIndex);
    }

    @Override
    public List<String> segments(LepMethod method) {
        String functionKey = method.getParameter(PARAM_FUNCTION_KEY, String.class);
        return List.of(functionKey.substring(functionKey.lastIndexOf('/') + 1));
    }
}
