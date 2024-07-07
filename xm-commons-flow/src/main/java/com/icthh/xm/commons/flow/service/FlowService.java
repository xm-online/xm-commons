package com.icthh.xm.commons.flow.service;

import com.icthh.xm.commons.config.client.repository.TenantConfigRepository;
import com.icthh.xm.commons.config.domain.Configuration;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.flow.domain.TenantResource;
import com.icthh.xm.commons.flow.domain.dto.Flow;
import com.icthh.xm.commons.flow.domain.dto.Step;
import com.icthh.xm.commons.flow.service.FlowConfigService.FlowsConfig;
import com.icthh.xm.commons.flow.service.resolver.FlowTypeLepKeyResolver;
import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

@Slf4j
@RequiredArgsConstructor
@Component
@LepService(group = "service.flow")
public class FlowService {

    private final FlowConfigService flowConfigService;
    private final YamlConverter yamlConverter;
    private final TenantConfigRepository tenantConfigRepository;
    private final CodeSnippetService codeSnippetService;

    @Setter(onMethod = @__(@Autowired))
    private FlowService self;

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
        self.modifyFlow(flow);
    }

    @LogicExtensionPoint(value = "UpdateFlow", resolver = FlowTypeLepKeyResolver.class)
    public void updateFlow(Flow flow) {
        assertExits(flow.getKey());
        self.modifyFlow(flow);
    }

    @LogicExtensionPoint(value = "ModifyFlow")
    public void modifyFlow(Flow flow) {
        Map<String, FlowsConfig> updatedConfigs = new HashMap<>();
        Map<String, FlowsConfig> configFiles = flowConfigService.copyFilesConfig();
        var configsWhereRemovedFlow = flowConfigService.removeFlow(configFiles, flow.getKey());
        var configsWhereAddedFlow = flowConfigService.updateFileConfiguration(configFiles, flow);
        updatedConfigs.putAll(configsWhereRemovedFlow);
        updatedConfigs.putAll(configsWhereAddedFlow);

        List<Configuration> configurations = convertToConfiguration(updatedConfigs);

        List<Configuration> snippets = codeSnippetService.generateSnippets(flow);
        configurations.addAll(snippets);

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
        log.info("Updated configs.size: {}", configurations.size());
        tenantConfigRepository.updateConfigurations(configurations);
    }

    private List<Configuration> convertToConfiguration(Map<String, FlowsConfig> updatedConfigs) {
        return updatedConfigs.entrySet().stream()
            .map(entry -> new Configuration(entry.getKey(), yamlConverter.writeConfig(entry.getValue())))
            .collect(toList());
    }

}
