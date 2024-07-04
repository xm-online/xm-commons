package com.icthh.xm.commons.flow.api;

import com.icthh.xm.commons.lep.api.BaseLepContext;

public interface Action {
    <T extends BaseLepContext & FlowLepContextFields> Object execute(T lepContext);
}
