package com.icthh.xm.commons.flow.rest;

import com.icthh.xm.commons.flow.service.FlowService;
import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/flow")
@RequiredArgsConstructor
public class FlowExecuteResource {

    private final FlowService flowService;

    @GetMapping("/{flowKey}/execute")
    @PreAuthorize("hasPermission({'flowKey': #flowKey, 'queryParams': #queryParams}, 'FLOW.EXECUTE.GET')")
    @PrivilegeDescription("Privilege to execute the flow by key using get method")
    public Object executeFlowGet(@PathVariable("flowKey") String flowKey, @RequestParam Map<String, String> queryParams) {
        return executeFlow(flowKey, queryParams);
    }

    @PutMapping("/{flowKey}/execute")
    @PreAuthorize("hasPermission({'flowKey': #flowKey, 'body': #body, 'queryParams': #queryParams}, 'FLOW.EXECUTE.PUT')")
    @PrivilegeDescription("Privilege to execute the flow by key using put method")
    public Object executeFlowPut(@PathVariable String flowKey, @RequestBody Map<String, Object> body) {
        return executeFlow(flowKey, body);
    }

    @PostMapping(path = "/{flowKey}/execute", consumes = "application/json")
    @PreAuthorize("hasPermission({'flowKey': #flowKey, 'body': #body, 'queryParams': #queryParams}, 'FLOW.EXECUTE.POST_JSON')")
    @PrivilegeDescription("Privilege to execute the flow by key using post json method")
    public Object executeFlowPostJson(@PathVariable String flowKey, @RequestBody Map<String, Object> body) {
        return executeFlow(flowKey, body);
    }

    @PostMapping(path = "/{flowKey}/execute", consumes = "application/x-www-form-urlencoded")
    @PreAuthorize("hasPermission({'flowKey': #flowKey, 'formData': #formData}, 'FLOW.EXECUTE.POST_FORM')")
    @PrivilegeDescription("Privilege to execute the flow by key using post url encoded method")
    public Object executeFlowPostUrlEncoded(@PathVariable String flowKey, @RequestParam Map<String, String> formData) {
        return executeFlow(flowKey, formData);
    }

    private Object executeFlow(String flowKey, Object body) {
        return flowService.runFlow(flowKey, body);
    }

}
