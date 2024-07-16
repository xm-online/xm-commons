import com.icthh.xm.commons.flow.spec.step.StepSpec

import java.util.concurrent.ConcurrentHashMap

def step = lepContext.lepServices.getInstance(StepResolver.class).resolve(lepContext, lepContext.inArgs.stepSpec)
return step.execute(lepContext)

class StepResolver {
    private final Map<String, Object> steps = new ConcurrentHashMap<>();

    public def resolve(def lepContext, StepSpec spec) {
        def step = steps.computeIfAbsent(spec.implementation) {
            lepContext.lepServices.getInstance(Class.forName(spec.implementation))
        }
        return step
    }

}
