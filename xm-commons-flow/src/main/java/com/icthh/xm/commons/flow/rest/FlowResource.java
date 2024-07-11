package com.icthh.xm.commons.flow.rest;

import com.icthh.xm.commons.flow.domain.Flow;
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
    public Flow getFlow(@PathVariable("flowKey") String flowKey) {
        return flowService.getFlow(flowKey);
    }

    @GetMapping()
    public List<Flow> getFlows() {
        return flowService.getFlows();
    }

    @PostMapping()
    public void createFlow(@RequestBody Flow flow) {
        flowService.createFlow(flow);
    }

    @PutMapping()
    public void updateFlow(@RequestBody Flow flow) {
        flowService.updateFlow(flow);
    }

    @DeleteMapping("/{flowKey}")
    public void deleteFlow(@PathVariable("flowKey") String flowKey) {
        flowService.deleteFlow(flowKey);
    }

}
