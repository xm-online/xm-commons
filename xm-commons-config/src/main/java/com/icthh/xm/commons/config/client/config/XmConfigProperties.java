package com.icthh.xm.commons.config.client.config;

import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.io.File.separator;
import static org.apache.commons.lang3.StringUtils.strip;

@Component
@ConditionalOnProperty("xm-config.enabled")
@ConfigurationProperties(prefix = "xm-config", ignoreUnknownFields = false)
@Data
public class XmConfigProperties {

    private Boolean enabled;
    private String xmConfigUrl = "http://config";
    private String tenantConfigPattern;
    private String kafkaConfigTopic = "config_topic";
    // XM_MS_CONFIG | FILE
    private String configMode = "XM_MS_CONFIG";
    private String configDirPath;
    private Integer dirWatchInterval = 500;

    private Set<String> includeTenants;

    public Set<String> getIncludeTenantLowercase() {
        return Optional.ofNullable(getIncludeTenants())
                       .orElse(new HashSet<>())
                       .stream()
                       .map(String::toLowerCase)
                       .collect(Collectors.toSet());
    }

    public String getDirectoryBasePath() {
        return separator + strip(configDirPath, separator);
    }
}
