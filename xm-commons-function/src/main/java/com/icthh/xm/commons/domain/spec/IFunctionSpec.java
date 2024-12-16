package com.icthh.xm.commons.domain.spec;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.icthh.xm.commons.domain.enums.FunctionTxTypes;

public interface IFunctionSpec {

    String getKey();
    String getPath();
    FunctionTxTypes getTxType();
    Boolean getAnonymous();
    @JsonIgnore
    Boolean getWrapResult();
}
