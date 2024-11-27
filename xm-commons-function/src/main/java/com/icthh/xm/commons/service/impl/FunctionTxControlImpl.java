package com.icthh.xm.commons.service.impl;

import com.icthh.xm.commons.domain.FunctionResult;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.commons.service.FunctionTxControl;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Supplier;

@Component
public class FunctionTxControlImpl implements FunctionTxControl {

    @Override
    @Transactional
    @IgnoreLogginAspect
    public FunctionResult executeInTransaction(Supplier<FunctionResult> executor) {
        return executor.get();
    }

    @Override
    @Transactional(readOnly = true)
    @IgnoreLogginAspect
    public FunctionResult executeInTransactionWithRoMode(Supplier<FunctionResult> executor) {
        return executor.get();
    }

    @Override
    @IgnoreLogginAspect
    public FunctionResult executeWithNoTx(Supplier<FunctionResult> executor) {
        return executor.get();
    }
}
