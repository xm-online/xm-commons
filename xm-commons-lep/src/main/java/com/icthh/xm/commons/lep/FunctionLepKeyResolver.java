package com.icthh.xm.commons.lep;

import com.icthh.xm.lep.api.LepManagerService;
import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.lep.api.commons.SeparatorSegmentedLepKey;

public class FunctionLepKeyResolver extends AppendLepKeyResolver {

    /**
     * Method parameter name for {@code functionKey}.
     */
    private static final String PARAM_FUNCTION_KEY = "functionKey";

    /**
     * {@inheritDoc}
     */
    @Override
    protected String[] getAppendSegments(SeparatorSegmentedLepKey baseKey,
                                         LepMethod method,
                                         LepManagerService managerService) {
        // function key
        String translatedFuncKey = translateToLepConvention(getRequiredStrParam(method, PARAM_FUNCTION_KEY));
        return new String[]{translatedFuncKey};
    }
}
