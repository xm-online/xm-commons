package com.icthh.xm.commons.flow.service;

import com.icthh.xm.commons.config.client.repository.TenantConfigRepository;
import com.icthh.xm.commons.config.domain.Configuration;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.exceptions.EntityNotFoundException;
import com.icthh.xm.commons.flow.domain.Flow;
import com.icthh.xm.commons.flow.engine.FlowExecutor;
import com.icthh.xm.commons.flow.engine.context.FlowExecutionContext;
import com.icthh.xm.commons.flow.service.FlowConfigService.FlowsConfig;
import com.icthh.xm.commons.flow.service.resolver.FlowKeyLepKeyResolver;
import com.icthh.xm.commons.flow.service.resolver.FlowTypeLepKeyResolver;
import com.icthh.xm.commons.flow.service.trigger.TriggerProcessor;
import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
@RequiredArgsConstructor
@Component
@LepService(group = "service.flow")
public class FlowService {

    private final FlowConfigService flowConfigService;
    private final YamlConverter yamlConverter;
    private final TenantConfigRepository tenantConfigRepository;
    private final CodeSnippetService codeSnippetService;
    private final TriggerProcessor triggerProcessor;
    private final FlowExecutor flowExecutor;

    private FlowService self;
    @Autowired
    public void setFlowService(FlowService flowService) {
        this.self = flowService;
    }

    @LogicExtensionPoint("GetFlows")
    public List<Flow> getFlows() {
        return flowConfigService.getFlows();
    }

    @LogicExtensionPoint("GetFlow")
    public Flow getFlow(String flowKey) {
        return flowConfigService.getFlow(flowKey);
    }

    @LogicExtensionPoint(value = "CreateFlow", resolver = FlowTypeLepKeyResolver.class)
    public void createFlow(Flow flow) {
        assertNotExits(flow);
        assertStartStepExists(flow);
        self.saveFlowInternal(flow);
    }

    @LogicExtensionPoint(value = "UpdateFlow", resolver = FlowTypeLepKeyResolver.class)
    public void updateFlow(Flow flow) {
        assertExits(flow.getKey());
        assertStartStepExists(flow);
        self.saveFlowInternal(flow);
    }

    @LogicExtensionPoint(value = "SaveFlow")
    public void saveFlowInternal(Flow flow) {
        Map<String, FlowsConfig> updatedConfigs = new HashMap<>();
        Map<String, FlowsConfig> configFiles = flowConfigService.copyFilesConfig();
        var configsWhereRemovedFlow = flowConfigService.removeFlow(configFiles, flow.getKey());
        var configsWhereAddedFlow = flowConfigService.updateFileConfiguration(configFiles, flow);
        updatedConfigs.putAll(configsWhereRemovedFlow);
        updatedConfigs.putAll(configsWhereAddedFlow);

        List<Configuration> configurations = convertToConfiguration(updatedConfigs);

        List<Configuration> snippets = codeSnippetService.generateSnippets(flow);
        configurations.addAll(snippets);
        if (flow.getTrigger() != null) {
            List<Configuration> triggers = triggerProcessor.processTriggerUpdate(flow.getTrigger());
            configurations.addAll(triggers);
        }

        updateConfigurations(configurations);
    }

    @LogicExtensionPoint("DeleteFlow")
    public void deleteFlow(String flowKey) {
        Flow flow = flowConfigService.getFlow(flowKey);
        Map<String, FlowsConfig> configFiles = flowConfigService.copyFilesConfig();
        Map<String, FlowsConfig> filesWithFlow = flowConfigService.removeFlow(configFiles, flowKey);
        List<Configuration> configurations = convertToConfiguration(filesWithFlow);
        if (flow != null) {
            List<Configuration> snippets = codeSnippetService.generateSnippets(flow);
            snippets.forEach(snippet -> snippet.setContent(null));
            configurations.addAll(snippets);

            if (flow.getTrigger() != null) {
                List<Configuration> triggers = triggerProcessor.processTriggerDelete(flow.getTrigger());
                configurations.addAll(triggers);
            }
        }

        updateConfigurations(configurations);
    }

    private void assertNotExits(Flow flow) {
        if (flowConfigService.getFlow(flow.getKey()) != null) {
            throw new BusinessException("error.flow.already.exists", "Flow with key " + flow.getKey() + " already exists");
        }
    }

    private void assertExits(String flowKey) {
        if (flowConfigService.getFlow(flowKey) == null) {
            throw new BusinessException("error.flow.not.found", "Resource with key " + flowKey + " not found");
        }
    }

    private void updateConfigurations(List<Configuration> configurations) {
        log.debug("Updated configs: {}", configurations);
        log.info("Updated configs.count: {}", configurations.size());
        tenantConfigRepository.updateConfigurations(configurations);
    }

    private List<Configuration> convertToConfiguration(Map<String, FlowsConfig> updatedConfigs) {
        return updatedConfigs.entrySet().stream()
            .map(entry -> new Configuration(entry.getKey(), yamlConverter.writeConfig(entry.getValue())))
            .collect(toList());
    }

    private void assertStartStepExists(Flow flow) {
        boolean isExists = isNotBlank(flow.getStartStep()) && flow.getSteps().stream().anyMatch(step ->
            flow.getStartStep().equals(step.getKey())
        );

        if (!isExists) {
            throw new BusinessException("error.flow.start.step.not.found", "Start step with key " + flow.getStartStep() + " not found");
        }
    }

    @LogicExtensionPoint(value = "RunFlow", resolver = FlowKeyLepKeyResolver.class)
    public Object runFlow(String flowKey, Object input) {
        return self.runFlowInternal(flowKey, input).getOutput();
    }

    @LogicExtensionPoint("RunFlow")
    public FlowExecutionContext runFlowInternal(String flowKey, Object input) {
        Flow flow = getRequiredFlow(flowKey);
        FlowExecutionContext executionContext = self.executeFlow(flow, input);
        if (log.isDebugEnabled()) {
            log.debug("Flow execution context: {}", executionContext);
        }
        return executionContext;
    }

    @LogicExtensionPoint(value = "ExecuteFlow", resolver = FlowTypeLepKeyResolver.class)
    public FlowExecutionContext executeFlow(Flow flow, Object input) {
        return self.executeFlowInternal(flow, input);
    }

    @LogicExtensionPoint(value = "ExecuteFlow")
    public FlowExecutionContext executeFlowInternal(Flow flow, Object input) {
        return flowExecutor.execute(flow, input);
    }

    private Flow getRequiredFlow(String flowKey) {
        return Optional.ofNullable(flowConfigService.getFlow(flowKey))
            .orElseThrow(() -> new EntityNotFoundException("Flow with key " + flowKey + " not found"));
    }

}
