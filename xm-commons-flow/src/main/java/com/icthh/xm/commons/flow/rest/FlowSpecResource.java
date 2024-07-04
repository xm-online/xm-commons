package com.icthh.xm.commons.flow.rest;

import com.icthh.xm.commons.flow.spec.resource.TenantResourceType;
import com.icthh.xm.commons.flow.spec.resource.TenantResourceTypeService;
import com.icthh.xm.commons.flow.spec.step.StepSpec;
import com.icthh.xm.commons.flow.spec.step.StepSpec.StepType;
import com.icthh.xm.commons.flow.spec.step.StepSpecService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/flow/spec")
@RequiredArgsConstructor
public class FlowSpecResource {

    private final StepSpecService stepSpecService;
    private final TenantResourceTypeService resourceTypeService;

    @GetMapping("/steps")
    public List<StepSpec> getSteps(@RequestParam(name = "stepType", required = false) StepType stepType) {
        return stepSpecService.getSteps(stepType);
    }

    @GetMapping("/resource-types")
    public List<TenantResourceType> getResourceTypes() {
        return resourceTypeService.resourceTypes();
    }

}
