package com.icthh.xm.commons.domain.spec;

import com.icthh.xm.commons.domain.enums.FunctionTxTypes;

public abstract class IFunctionSpec {

    public abstract String getKey();
    public abstract String getPath();
    public abstract FunctionTxTypes getTxType();
    public abstract Boolean getAnonymous();
}
