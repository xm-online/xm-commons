package com.icthh.xm.commons.logging.aop;

import com.icthh.xm.commons.logging.LoggingAspectConfig;
import org.springframework.stereotype.Service;

import static com.icthh.xm.commons.logging.util.LogObjectPrinter.Level;

@Service
public class TestServiceWithLogLevel {

    @LoggingAspectConfig(logLevel = Level.DEBUG)
    public String testMethod(String input) {
        return "ok";
    }

    public String testDefault(String input) {
        return "ok";
    }
}
