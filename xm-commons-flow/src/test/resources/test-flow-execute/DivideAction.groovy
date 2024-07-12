package commons.lep.flow.actions

import com.icthh.xm.commons.flow.api.Action
import com.icthh.xm.commons.flow.api.FlowLepContextFields
import com.icthh.xm.commons.lep.api.BaseLepContext

class DivideAction implements Action {
    @Override
    <T extends BaseLepContext & FlowLepContextFields> Object execute(T lepContext) {
        return (int) (lepContext.step.parameters.c / lepContext.steps[lepContext.step.parameters.stepKey].output)
    }
}
