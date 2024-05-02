package com.icthh.xm.commons.lep.groovy;

import com.icthh.xm.commons.lep.TargetProceedingLep;
import com.icthh.xm.commons.lep.api.BaseLepContext;
import com.icthh.xm.commons.lep.api.LepEngine;
import com.icthh.xm.commons.lep.spring.LepContextCustomizer;
import com.icthh.xm.commons.lep.spring.LepContextService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

@Slf4j
@RequiredArgsConstructor
public class GroovyMapLepWrapperFactory implements LepContextCustomizer {

    private static final MethodHandle constructorHandle;

    static {
        constructorHandle = buildConstructorHandle();
    }

    private static MethodHandle buildConstructorHandle() {
        try {
            Class<?> mapWrapperClass = Class.forName("com.icthh.xm.commons.GroovyMapLepContextWrapper");

            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodType methodType = MethodType.methodType(void.class, BaseLepContext.class);
            return lookup.findConstructor(mapWrapperClass, methodType);
        } catch (NoSuchMethodException | IllegalAccessException | ClassNotFoundException e) {
            log.error("Groovy lepContext Map wrapper is not available. Add to gradle \"annotationProcessor 'com.icthh.xm.commons:xm-commons-lep-annotation-processor:${xm_commons_version}'\"");
        }
        return null;
    }

    @Override
    @SneakyThrows
    public BaseLepContext customize(BaseLepContext lepContext, LepEngine lepEngine, TargetProceedingLep lepMethod) {
        if (constructorHandle != null && lepEngine instanceof GroovyLepEngine) {
            return (BaseLepContext) constructorHandle.invoke(lepContext);
        } else {
            return lepContext;
        }
    }
}
