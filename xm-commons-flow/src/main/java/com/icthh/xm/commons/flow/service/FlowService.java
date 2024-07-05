package com.icthh.xm.commons.flow.service;

import com.icthh.xm.commons.config.client.repository.TenantConfigRepository;
import com.icthh.xm.commons.config.domain.Configuration;
import com.icthh.xm.commons.flow.domain.dto.FlowDto;
import com.icthh.xm.commons.flow.service.FlowConfigService.FlowsConfig;
import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

@Slf4j
@RequiredArgsConstructor
@LepService(group = "service.flow")
public class FlowService {

    private final FlowConfigService flowConfigService;
    private final YamlConverter yamlConverter;
    private final TenantConfigRepository tenantConfigRepository;

    public void createFlow(FlowDto flow) {
        modifyFlow(flow);
    }

    public void updateFlow(FlowDto flow) {
        modifyFlow(flow);
    }

    private void modifyFlow(FlowDto flow) {
        Map<String, FlowsConfig> updatedConfigs = new HashMap<>();
        Map<String, FlowsConfig> configFiles = flowConfigService.copyFilesConfig();
        var configsWhereRemovedFlow = flowConfigService.removeFlow(configFiles, flow.getKey());
        var configsWhereAddedFlow = flowConfigService.updateFileConfiguration(configFiles, flow);
        updatedConfigs.putAll(configsWhereRemovedFlow);
        updatedConfigs.putAll(configsWhereAddedFlow);
        updateConfigurations(updatedConfigs);
    }

    @LogicExtensionPoint("DeleteFlow")
    public void deleteFlow(String flowKey) {
        Map<String, FlowsConfig> configFiles = flowConfigService.copyFilesConfig();
        Map<String, FlowsConfig> filesWithFlow = flowConfigService.removeFlow(configFiles, flowKey);
        filesWithFlow.forEach((file, config) -> {
            config.getFlows().removeIf(flow -> flow.getKey().equals(flowKey));
        });
        updateConfigurations(filesWithFlow);
    }

    @LogicExtensionPoint("GetFlows")
    public List<FlowDto> getFlows() {
        return flowConfigService.getFlows();
    }

    @LogicExtensionPoint("GetFlow")
    public FlowDto getFlow(String flowKey) {
        return flowConfigService.getFlow(flowKey);
    }

    private void updateConfigurations(Map<String, FlowConfigService.FlowsConfig> updatedConfigs) {
        log.debug("Updated configs: {}", updatedConfigs);
        log.info("Updated configs.size: {}", updatedConfigs.size());
        List<Configuration> configurations = updatedConfigs.entrySet().stream()
            .map(entry -> new Configuration(entry.getKey(), yamlConverter.writeConfig(entry.getValue())))
            .collect(toList());
        tenantConfigRepository.updateConfigurations(configurations);
    }

}
