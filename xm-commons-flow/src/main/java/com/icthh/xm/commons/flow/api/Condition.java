package com.icthh.xm.commons.flow.api;

import com.icthh.xm.commons.lep.api.BaseLepContext;

public interface Condition extends Action {
    @Override
    default <T extends BaseLepContext & FlowLepContextFields> Boolean execute(T lepContext) {
        return test(lepContext);
    }
    <T extends BaseLepContext & FlowLepContextFields> Boolean test(T lepContext);
}
