package com.icthh.xm.commons.logging.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.icthh.xm.commons.logging.util.LogObjectPrinter;
import lombok.Data;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.icthh.xm.commons.logging.util.LogObjectPrinter.*;

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

    public Map<String, LepLogConfiguration> buildLepLoggingConfigs(String tenantKey) {
        Map<String, LepLogConfiguration> configs = new HashMap<>();
        if (CollectionUtils.isEmpty(lepLogConfigurations)) {
            return configs;
        }
        lepLogConfigurations.forEach(lepLogConfiguration -> {
            String pathKey = "lep://" + tenantKey.toUpperCase() + "/"
                + lepLogConfiguration.getGroup() + "/"
                + lepLogConfiguration.getFileName();

            configs.put(pathKey, lepLogConfiguration);
        });
        return configs;
    }

    private Map<String, LogConfiguration> buildLogConfiguration(List<LogConfiguration> logConfigurations) {
        Map<String, LogConfiguration> configs = new HashMap<>();
        if (CollectionUtils.isEmpty(logConfigurations)) {
            return configs;
        }

        logConfigurations.forEach(it -> {
            if (StringUtils.isNotBlank(it.getClassName())) {
                if (StringUtils.isNotBlank(it.getMethodName())) {
                    if (StringUtils.isNotBlank(it.getPackageName())) {
                        configs.put(it.getPackageName() + ":" + it.getClassName() + ":" + it.getMethodName(), it);
                    } else {
                        configs.put(it.getClassName() + ":" + it.getMethodName(), it);
                    }
                } else {
                    configs.put(it.getClassName(), it);
                }
            }
        });
        return configs;
    }

    @Data
    public static class LepLogConfiguration {
        private String group;
        private String fileName;
        private Level level;
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
    }
}
