package com.icthh.xm.commons.lep;

import com.icthh.xm.lep.api.LepInvocationCauseException;
import com.icthh.xm.lep.api.LepManagerService;
import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.lep.api.commons.UrlLepResourceKey;
import com.icthh.xm.lep.groovy.GroovyScriptRunner;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * The {@link ScriptLepResourceProceedingLep} class.
 */
@Slf4j
public class ScriptLepResourceProceedingLep extends BaseProceedingLep {

    private final UrlLepResourceKey scriptResourceKey;
    private final UrlLepResourceKey compositeResourceKey;
    private final LepManagerService managerService;
    private final Supplier<GroovyScriptRunner> resourceExecutorSupplier;

    public ScriptLepResourceProceedingLep(UrlLepResourceKey compositeResourceKey,
                                          UrlLepResourceKey scriptResourceKey,
                                          LepMethod lepMethod,
                                          LepManagerService managerService,
                                          Supplier<GroovyScriptRunner> resourceExecutorSupplier) {
        super(lepMethod);

        this.scriptResourceKey = Objects.requireNonNull(scriptResourceKey,
                                                        "scriptResourceKey can't be null");

        this.compositeResourceKey = compositeResourceKey;
        this.managerService = managerService;
        this.resourceExecutorSupplier = resourceExecutorSupplier;
    }

    @Override
    public Object proceed() throws Exception {
        return invoke();
    }

    @Override
    public Object proceed(Object[] args) throws Exception {
        return invoke(args);
    }

    private Object invoke(Object... args) throws Exception {
        try {
            return LepScriptUtils.executeScript(scriptResourceKey,
                                                null,
                                                this,
                                                managerService,
                                                resourceExecutorSupplier,
                                                null,
                                                args);
        } catch (LepInvocationCauseException e) {
            log.warn("Error execute LEP {} script {}", compositeResourceKey, scriptResourceKey, e);
            throw e.getCause();
        }
    }
}
