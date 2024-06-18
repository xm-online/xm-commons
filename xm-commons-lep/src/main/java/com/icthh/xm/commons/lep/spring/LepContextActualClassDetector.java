package com.icthh.xm.commons.lep.spring;

import com.icthh.xm.commons.lep.api.BaseLepContext;

public interface LepContextActualClassDetector {
    Class<? extends BaseLepContext> detectActualClass();
}
