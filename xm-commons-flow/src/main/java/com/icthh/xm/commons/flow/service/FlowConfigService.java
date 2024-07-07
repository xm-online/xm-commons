package com.icthh.xm.commons.flow.service;

import com.icthh.xm.commons.config.client.api.refreshable.MapRefreshableConfiguration;
import com.icthh.xm.commons.flow.domain.dto.Flow;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static java.util.stream.Collectors.toUnmodifiableMap;

@Component
public class FlowConfigService extends MapRefreshableConfiguration<Flow, FlowConfigService.FlowsConfig> {

    public FlowConfigService(@Value("${spring.application.name}") String appName,
                             TenantContextHolder tenantContextHolder) {
        super(appName, tenantContextHolder);
    }

    @Override
    protected List<Flow> toConfigItems(FlowsConfig config) {
        return config.getFlows();
    }

    @Override
    public Class<FlowsConfig> configFileClass() {
        return FlowsConfig.class;
    }

    @Override
    public String configName() {
        return "flow";
    }

    public List<Flow> getFlows() {
        return List.copyOf(getConfiguration().values());
    }

    public Flow getFlow(String flowKey) {
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
            List<Flow> flow = config.getFlows();
            boolean contains = flow.stream().anyMatch(it -> it.getKey().equals(flowKey));
            if (contains) {
                updatedFiles.put(file, config);
                flow.removeIf(it -> it.getKey().equals(flowKey));
            }
        });
        return updatedFiles;
    }

    public Map<String, FlowsConfig> updateFileConfiguration(Map<String, FlowsConfig> configurationFiles,
                                                            Flow flow) {
        String filePath = buildFilePath(flow.getKey());
        FlowsConfig resourceFile = configurationFiles.get(filePath);
        if (resourceFile != null) {
            resourceFile.getFlows().add(flow);
        } else {
            resourceFile = new FlowsConfig();
            resourceFile.setFlows(List.of(flow));
        }
        return Map.of(filePath, resourceFile);
    }

    @Data
    public static class FlowsConfig {
        private List<Flow> flows;

        public List<Flow> getFlows() {
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
