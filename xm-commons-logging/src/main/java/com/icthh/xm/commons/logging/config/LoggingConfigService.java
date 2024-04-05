package com.icthh.xm.commons.logging.config;

import com.icthh.xm.commons.logging.config.LoggingConfig.LepLogConfiguration;
import com.icthh.xm.commons.logging.util.MaskingService;

import static com.icthh.xm.commons.logging.config.LoggingConfig.LogConfiguration;

public interface LoggingConfigService {

    MaskingService getMaskingService();

    LogConfiguration getServiceLoggingConfig(String packageName, String className, String methodName);

    LogConfiguration getApiLoggingConfig(String packageName, String className, String methodName);

    LepLogConfiguration getLepLoggingConfig(String fileName);

}
