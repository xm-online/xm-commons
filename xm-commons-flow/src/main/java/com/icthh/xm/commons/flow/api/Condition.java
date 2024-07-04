package com.icthh.xm.commons.flow.api;

import com.icthh.xm.commons.lep.api.BaseLepContext;

public interface Condition {
    <T extends BaseLepContext & FlowLepContextFields> Boolean checkCondition(T lepContext);
}
