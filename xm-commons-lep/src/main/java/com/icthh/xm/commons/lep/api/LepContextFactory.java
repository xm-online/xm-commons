package com.icthh.xm.commons.lep.api;

import com.icthh.xm.lep.api.LepMethod;

public interface LepContextFactory {
    BaseLepContext buildLepContext(LepMethod lepMethod);
}
