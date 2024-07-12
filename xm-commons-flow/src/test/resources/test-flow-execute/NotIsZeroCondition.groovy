package commons.lep.flow.conditions

import com.icthh.xm.commons.flow.api.Condition
import com.icthh.xm.commons.flow.api.FlowLepContextFields
import com.icthh.xm.commons.lep.api.BaseLepContext

class NotIsZeroCondition implements Condition {
    @Override
    <T extends BaseLepContext & FlowLepContextFields> Boolean test(T lepContext) {
        return lepContext.steps[lepContext.step.parameters.stepKey].output != 0
    }
}
