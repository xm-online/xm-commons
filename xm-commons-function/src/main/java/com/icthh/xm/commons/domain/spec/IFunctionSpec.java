package com.icthh.xm.commons.domain.spec;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.icthh.xm.commons.domain.enums.FunctionTxTypes;

import java.util.List;

public interface IFunctionSpec {

    String getKey();
    String getPath();
    List<String> getHttpMethods();
    FunctionTxTypes getTxType();
    Boolean getAnonymous();
    @JsonIgnore
    Boolean getWrapResult();
}
