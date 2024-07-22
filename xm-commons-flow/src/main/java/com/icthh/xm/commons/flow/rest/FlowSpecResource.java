package com.icthh.xm.commons.flow.rest;

import com.icthh.xm.commons.flow.spec.resource.TenantResourceType;
import com.icthh.xm.commons.flow.spec.resource.TenantResourceTypeService;
import com.icthh.xm.commons.flow.spec.step.StepSpec;
import com.icthh.xm.commons.flow.spec.step.StepSpecService;
import com.icthh.xm.commons.flow.spec.trigger.TriggerType;
import com.icthh.xm.commons.flow.spec.trigger.TriggerTypeSpecService;
import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PostAuthorize;
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
    private final TriggerTypeSpecService triggerSpecService;

    @GetMapping("/steps")
    @PostAuthorize("hasPermission({'returnObject': returnObject}, 'FLOW.STEP_SPEC.GET_LIST')")
    @PrivilegeDescription("Privilege to get all step specs")
    public List<StepSpec> getSteps(@RequestParam(name = "stepType", required = false) StepSpec.StepType stepType) {
        return stepSpecService.getSteps(stepType);
    }

    @GetMapping("/resource-types")
    @PostAuthorize("hasPermission({'returnObject': returnObject}, 'FLOW.RESOURCE_TYPE.GET_LIST')")
    @PrivilegeDescription("Privilege to get all resource types")
    public List<TenantResourceType> getResourceTypes() {
        return resourceTypeService.resourceTypes();
    }

    @GetMapping("/trigger-types")
    @PostAuthorize("hasPermission({'returnObject': returnObject}, 'FLOW.TRIGGER_TYPE.GET_LIST')")
    @PrivilegeDescription("Privilege to get all trigger types")
    public List<TriggerType> getTriggerTypes() {
        return triggerSpecService.triggerTypes();
    }

}
