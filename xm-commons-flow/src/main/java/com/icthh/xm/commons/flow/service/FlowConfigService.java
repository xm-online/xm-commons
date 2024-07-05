package com.icthh.xm.commons.flow.service;

import com.icthh.xm.commons.config.client.api.refreshable.MapRefreshableConfiguration;
import com.icthh.xm.commons.flow.domain.dto.FlowDto;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static java.util.stream.Collectors.toUnmodifiableMap;

public class FlowConfigService extends MapRefreshableConfiguration<FlowDto, FlowConfigService.FlowsConfig> {

    public FlowConfigService(@Value("${spring.application.name}") String appName,
                             TenantContextHolder tenantContextHolder) {
        super(appName, tenantContextHolder);
    }

    @Override
    protected List<FlowDto> toConfigItems(FlowsConfig config) {
        return config.getFlows();
    }

    @Override
    public Class<FlowsConfig> configFileClass() {
        return FlowsConfig.class;
    }

    @Override
    public String configName() {
        return "flows";
    }

    public List<FlowDto> getFlows() {
        return List.copyOf(getConfiguration().values());
    }

    public FlowDto getFlow(String flowKey) {
        return getConfiguration().get(flowKey);
    }

    public Map<String, FlowsConfig>  copyFilesConfig() {
        Map<String, FlowsConfig> configurationFiles = getConfigurationFiles();
        return configurationFiles.entrySet().stream()
            .collect(toUnmodifiableMap(Entry::getKey, entry -> entry.getValue().copy()));
    }

    public Map<String, FlowsConfig> removeFlow(Map<String, FlowsConfig> configurationFiles,
                                               String flowKey) {
        Map<String, FlowsConfig> updatedFiles = new HashMap<>();
        configurationFiles.forEach((file, config) -> {
            List<FlowDto> flow = config.getFlows();
            boolean contains = flow.stream().anyMatch(it -> it.getKey().equals(flowKey));
            if (contains) {
                updatedFiles.put(file, config);
                flow.removeIf(it -> it.getKey().equals(flowKey));
            }
        });
        return updatedFiles;
    }

    public Map<String, FlowsConfig> updateFileConfiguration(Map<String, FlowsConfig> configurationFiles,
                                                            FlowDto flowDto) {
        String filePath = buildFilePath(flowDto.getKey());
        FlowsConfig resourceFile = configurationFiles.get(filePath);
        if (resourceFile != null) {
            resourceFile.getFlows().add(flowDto);
        } else {
            resourceFile = new FlowsConfig();
            resourceFile.setFlows(List.of(flowDto));
        }
        return Map.of(filePath, resourceFile);
    }

    @Data
    public static class FlowsConfig {
        private List<FlowDto> flows;

        public List<FlowDto> getFlows() {
            if (flows == null) {
                flows = new ArrayList<>();
            }
            return flows;
        }

        public FlowsConfig copy() {
            FlowsConfig copy = new FlowsConfig();
            copy.setFlows(new ArrayList<>(flows));
            return copy;
        }
    }
}
