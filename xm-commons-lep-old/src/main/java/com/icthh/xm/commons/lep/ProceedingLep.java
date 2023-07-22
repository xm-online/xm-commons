package com.icthh.xm.commons.lep;

import com.icthh.xm.lep.api.LepMethod;

/**
 * The {@link ProceedingLep} interface.
 */
public interface ProceedingLep extends LepMethod {

    Object proceed() throws Exception;

    Object proceed(Object[] args) throws Exception;

}
