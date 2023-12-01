package com.icthh.xm.commons.logging.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.icthh.xm.commons.logging.util.LogObjectPrinter.Level;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Data
public class LoggingConfig {

    public static final boolean DEFAULT_LOG_INPUT_DETAILS = true;
    public static final boolean DEFAULT_LOG_INPUT_COLLECTION_AWARE = false;
    public static final boolean DEFAULT_LOG_RESULT_DETAILS = true;
    public static final boolean DEFAULT_LOG_RESULT_COLLECTION_AWARE = false;

    @JsonProperty("service")
    private List<LogConfiguration> serviceLoggingConfigs;

    @JsonProperty("api")
    private List<LogConfiguration> apiLoggingConfigs;

    @JsonProperty("lep")
    private List<LepLogConfiguration> lepLogConfigurations;

    public Map<String, LogConfiguration> buildServiceLoggingConfigs() {
        return buildLogConfiguration(this.getServiceLoggingConfigs());
    }

    public Map<String, LogConfiguration> buildApiLoggingConfigs() {
        return buildLogConfiguration(this.getApiLoggingConfigs());
    }

    public Map<String, LepLogConfiguration> buildLepLoggingConfigs(String tenantKey, String appName) {
        List<LepLogConfiguration> lepLogConfigurations = this.getLepLogConfigurations();
        if (CollectionUtils.isEmpty(lepLogConfigurations)) {
            return Collections.emptyMap();
        }

        return lepLogConfigurations.stream().collect(toMap(configuration -> configuration.buildConfigKey(tenantKey, appName),
                                                           configuration -> configuration));
    }

    private Map<String, LogConfiguration> buildLogConfiguration(List<LogConfiguration> logConfigurations) {
        Map<String, LogConfiguration> configs = new HashMap<>();
        if (CollectionUtils.isEmpty(logConfigurations)) {
            return configs;
        }

       return logConfigurations.stream().collect(toMap(LogConfiguration::buildConfigKey,
                                                       configuration -> configuration));
    }

    @Data
    public static class LepLogConfiguration {
        private String group;
        private String fileName;
        private Level level;

        String buildConfigKey(String tenantKey, String appName) {
            return  "lep://" + tenantKey.toUpperCase() + "/" + appName + "/lep/" + this.getGroup() + "/" + this.getFileName();
        }
    }

    @Data
    public static class LogConfiguration {
        private String className;
        private String methodName;
        private String packageName;
        private Level level;
        private LogInput logInput = new LogInput();
        private LogResult logResult = new LogResult();

        @Data
        static public class LogInput {
            private Boolean details = DEFAULT_LOG_INPUT_DETAILS;
            private Boolean collectionAware = DEFAULT_LOG_INPUT_COLLECTION_AWARE;
            private List<String> includeParams = new ArrayList<>();
            private List<String> excludeParams = new ArrayList<>();
        }

        @Data
        static public class LogResult {
            private Boolean resultDetails = DEFAULT_LOG_RESULT_DETAILS;
            private Boolean resultCollectionAware = DEFAULT_LOG_RESULT_COLLECTION_AWARE;
        }

        String buildConfigKey() {
            String key = this.getClassName();
            if (isNotBlank(this.getPackageName())) {
                key = this.getPackageName() + ":" + key;
            }
            if (isNotBlank(this.getMethodName())) {
                key = key + ":" + this.getMethodName();
            }
            return key;
        }
    }
}
