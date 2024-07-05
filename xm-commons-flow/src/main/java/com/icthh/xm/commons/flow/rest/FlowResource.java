package com.icthh.xm.commons.flow.rest;

import com.icthh.xm.commons.flow.domain.TenantResource;
import com.icthh.xm.commons.flow.domain.dto.FlowDto;
import com.icthh.xm.commons.flow.service.FlowService;
import lombok.RequiredArgsConstructor;
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
    public FlowDto getFlow(@PathVariable("flowKey") String flowKey) {
        return flowService.getFlow(flowKey);
    }

    @GetMapping()
    public List<FlowDto> getFlows() {
        return flowService.getFlows();
    }

    @PostMapping()
    public void createFlow(@RequestBody FlowDto flow) {
        flowService.createFlow(flow);
    }

    @PutMapping()
    public void updateFlow(@RequestBody FlowDto flow) {
        flowService.updateFlow(flow);
    }

    @DeleteMapping("/{flowKey}")
    public void deleteFlow(@PathVariable("flowKey") String flowKey) {
        flowService.deleteFlow(flowKey);
    }

}
