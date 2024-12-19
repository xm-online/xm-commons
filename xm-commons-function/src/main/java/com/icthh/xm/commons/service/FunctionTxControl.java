package com.icthh.xm.commons.service;

import com.icthh.xm.commons.domain.FunctionResult;

import java.util.function.Supplier;

public interface FunctionTxControl {

    FunctionResult executeInTransaction(Supplier<FunctionResult> executor);

    FunctionResult executeInTransactionWithRoMode(Supplier<FunctionResult> executor);

    FunctionResult executeWithNoTx(Supplier<FunctionResult> executor);
}
