package com.icthh.xm.commons.logging.aop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public interface TestServiceForLogging {

    void onRefresh(String updatedKey, String config);

    boolean isListeningConfiguration(String updatedKey);

    void onInit(String configKey, String configValue);

    class TestServiceForLoggingImpl implements TestServiceForLogging {

        private static final Logger log = LoggerFactory.getLogger(TestServiceForLogging.class);

        @Override
        public void onRefresh(final String updatedKey, final String config) {
            log.info("run action for updatedKey = {}, config = {}", updatedKey, config);
        }

        @Override
        public boolean isListeningConfiguration(final String updatedKey) {
            return false;
        }

        @Override
        public void onInit(final String configKey, final String configValue) {

            onRefresh(configKey, configValue);

        }
    }

}
