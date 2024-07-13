package com.icthh.xm.commons.flow.rest;

import com.icthh.xm.commons.flow.domain.Flow;
import com.icthh.xm.commons.flow.service.FlowService;
import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/flow")
@RequiredArgsConstructor
public class FlowResource {

    private final FlowService flowService;

    @GetMapping("/{flowKey}")
    @PostAuthorize("hasPermission({'returnObject': returnObject.body}, 'FLOW.GET_ITEM')")
    @PrivilegeDescription("Privilege to get the flow by key")
    public Flow getFlow(@PathVariable("flowKey") String flowKey) {
        return flowService.getFlow(flowKey);
    }

    @GetMapping()
    @PostAuthorize("hasPermission({'returnObject': returnObject.body}, 'FLOW.GET_LIST')")
    @PrivilegeDescription("Privilege to get all flows")
    public List<Flow> getFlows() {
        return flowService.getFlows();
    }

    @PostMapping()
    @PreAuthorize("hasPermission({'flow': #flow}, 'FLOW.CREATE')")
    @PrivilegeDescription("Privilege to create a new flow")
    public void createFlow(@RequestBody Flow flow) {
        flowService.createFlow(flow);
    }

    @PutMapping()
    @PreAuthorize("hasPermission({'flow': #flow}, 'FLOW.UPDATE')")
    @PrivilegeDescription("Privilege to update the flow")
    public void updateFlow(@RequestBody Flow flow) {
        flowService.updateFlow(flow);
    }

    @DeleteMapping("/{flowKey}")
    @PreAuthorize("hasPermission({'flowKey': #flowKey}, 'FLOW.DELETE')")
    @PrivilegeDescription("Privilege to delete the flow")
    public void deleteFlow(@PathVariable("flowKey") String flowKey) {
        flowService.deleteFlow(flowKey);
    }

}
