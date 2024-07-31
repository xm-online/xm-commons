package com.icthh.xm.commons.lep.spring;

import com.icthh.xm.commons.lep.TargetProceedingLep;
import com.icthh.xm.commons.lep.api.BaseLepContext;
import com.icthh.xm.commons.lep.api.LepAdditionalContext;
import com.icthh.xm.commons.lep.api.LepContextFactory;
import com.icthh.xm.commons.lep.api.LepEngine;
import com.icthh.xm.commons.lep.commons.CommonsExecutor;
import com.icthh.xm.commons.lep.commons.CommonsService;
import com.icthh.xm.commons.lep.spring.lepservice.LepServiceFactoryImpl;
import com.icthh.xm.commons.lep.spring.lepservice.LepServiceFactoryWithLepFactoryMethod;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.lep.api.LepMethod;
import lombok.RequiredArgsConstructor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class LepContextServiceImpl implements LepContextService {

    private final LepContextFactory lepContextFactory;
    private final LepServiceFactoryWithLepFactoryMethod lepServiceFactory;
    private final LepThreadHelper lepThreadHelper;
    private final TenantContextHolder tenantContextHolder;
    private final XmAuthenticationContextHolder xmAuthContextHolder;
    private final List<LepAdditionalContext<?>> additionalContexts;
    private final List<LepContextCustomizer> customizers;
    private final CommonsService commonsService;

    @Override
    public final BaseLepContext createLepContext(LepEngine lepEngine, TargetProceedingLep lepMethod) {
        BaseLepContext baseLepContext = lepContextFactory.buildLepContext(lepMethod);
        buildUpInVars(lepMethod, baseLepContext);
        baseLepContext.lep = lepMethod;
        baseLepContext.thread = lepThreadHelper;
        baseLepContext.tenantContext = tenantContextHolder.getContext();
        baseLepContext.authContext = xmAuthContextHolder.getContext();
        baseLepContext.authContextHolder = xmAuthContextHolder;
        baseLepContext.commons = new CommonsExecutor(commonsService);
        additionalContexts.forEach(context -> {
                Object value = context.additionalContextValue(baseLepContext, lepEngine, lepMethod)
                    .map(Object.class::cast)
                    .orElse(context.additionalContextValue());
                baseLepContext.addAdditionalContext(context.additionalContextKey(), value);
            }
        );
        baseLepContext.lepServices = new LepServiceFactoryImpl(lepEngine.getId(), lepServiceFactory);

        return customize(baseLepContext, lepEngine, lepMethod);
    }

    private BaseLepContext customize(BaseLepContext lepContext, LepEngine lepEngine, TargetProceedingLep lepMethod) {
        for (var customizer : customizers) {
            lepContext = customizer.customize(lepContext, lepEngine, lepMethod);
        }
        return lepContext;
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
