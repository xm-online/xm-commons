package com.icthh.xm.commons.lep.impl;

import com.icthh.xm.commons.lep.TargetProceedingLep;
import com.icthh.xm.commons.lep.api.BaseLepContext;
import com.icthh.xm.commons.lep.api.LepAdditionalContext;
import com.icthh.xm.commons.lep.api.LepContextFactory;
import com.icthh.xm.commons.lep.commons.CommonsExecutor;
import com.icthh.xm.commons.lep.commons.CommonsService;
import com.icthh.xm.commons.lep.spring.LepThreadHelper;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.lep.api.LepMethod;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class LepContextService {

    private final LepContextFactory lepContextFactory;
    private final LepThreadHelper lepThreadHelper;
    private final TenantContextHolder tenantContextHolder;
    private final XmAuthenticationContextHolder xmAuthContextHolder;
    private final List<LepAdditionalContext<?>> additionalContexts;
    private final CommonsService commonsService;

    public final BaseLepContext createLepContext(TargetProceedingLep lepMethod) {
        BaseLepContext baseLepContext = lepContextFactory.buildLepContext(lepMethod);
        buildUpInVars(lepMethod, baseLepContext);
        baseLepContext.lep = lepMethod;
        baseLepContext.thread = lepThreadHelper;
        baseLepContext.tenantContext = tenantContextHolder.getContext();
        baseLepContext.authContext = xmAuthContextHolder.getContext();
        baseLepContext.commons = new CommonsExecutor(commonsService);
        additionalContexts.forEach(context ->
            baseLepContext.addAdditionalContext(context.additionalContextKey(), context.additionalContextValue()));

        return baseLepContext;
    }

    private void buildUpInVars(LepMethod lepMethod, BaseLepContext baseLepContext) {
        final String[] parameterNames = lepMethod.getMethodSignature().getParameterNames();
        final Object[] methodArgValues = lepMethod.getMethodArgValues();
        Map<String, Object> inVars = new LinkedHashMap<>(parameterNames.length);
        for (int i = 0; i < parameterNames.length; i++) {
            String paramName = parameterNames[i];
            Object paramValue = methodArgValues[i];
            inVars.put(paramName, paramValue);
        }
        baseLepContext.inArgs = inVars;
    }

}
