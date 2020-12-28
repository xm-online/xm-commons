package com.icthh.xm.commons.logging.config;

import com.icthh.xm.commons.logging.config.LoggingConfig.LepLogConfiguration;

import java.util.Optional;

import static com.icthh.xm.commons.logging.config.LoggingConfig.LogConfiguration;

public interface LoggingConfigService {

    LogConfiguration getServiceLoggingConfig(String packageName, String className, String methodName);

    LogConfiguration getApiLoggingConfig(String packageName, String className, String methodName);

    LepLogConfiguration getLepLoggingConfig(String fileName);

}
