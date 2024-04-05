package com.icthh.xm.commons.logging.config;

import com.icthh.xm.commons.logging.util.MaskingService;

import static com.icthh.xm.commons.logging.config.LoggingConfig.LepLogConfiguration;
import static com.icthh.xm.commons.logging.config.LoggingConfig.LogConfiguration;

public class LoggingConfigServiceStub implements LoggingConfigService {

    @Override
    public MaskingService getMaskingService() {
        return null;
    }

    @Override
    public LogConfiguration getServiceLoggingConfig(String packageName, String className, String methodName) {
        return null;
    }

    @Override
    public LogConfiguration getApiLoggingConfig(String packageName, String className, String methodName) {
        return null;
    }

    @Override
    public LepLogConfiguration getLepLoggingConfig(String fileName) {
        return null;
    }
}
