package com.icthh.xm.commons.logging.configurable;

import com.icthh.xm.commons.security.spring.config.XmAuthenticationContextConfiguration;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.spring.config.TenantContextConfiguration;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.OutputCaptureRule;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.Assert.assertEquals;

@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {
    TenantContextConfiguration.class,
    LoggingRefreshableConfiguration.class,
    XmAuthenticationContextConfiguration.class,
    LogMaskingConfiguration.class
}, properties = {"spring.application.name=testApp"})
public class LogMaskingIntTest {

    private static final String UPDATE_KEY = "/config/tenants/TESTWITHCONFIG/testApp/logging.yml";

    @Autowired
    private TenantContextHolder tenantContextHolder;
    @Autowired
    private LoggingRefreshableConfiguration loggingRefreshableConfiguration;

    @Test
    public void testLogMaskingConfiguration() {
        String config = "masking:\n  enabled: true\n  maskPatterns:\n    - \"token=([^,]+)\"\n    - \"password=([^,]+)\"\n";
        loggingRefreshableConfiguration.onRefresh(UPDATE_KEY, config);
        String originalLog = "Log with password=passwordvalue, token=tokenvalue, attribute=blabla";
        String maskedLog =   "Log with password=*************, token=**********, attribute=blabla";

        writeLogExpectLog(originalLog, originalLog);

        TenantContextUtils.setTenant(tenantContextHolder, "TEST");
        writeLogExpectLog(originalLog, originalLog);
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();

        TenantContextUtils.setTenant(tenantContextHolder, "TESTWITHCONFIG");
        writeLogExpectLog(originalLog, maskedLog);
    }

    @SneakyThrows
    private void writeLogExpectLog(String logToWrite, String expectLog) {
        String loggerName = LogMaskingIntTest.class.getSimpleName() + ": ";
        var outputCapture = new OutputCaptureRule();
        outputCapture.apply(new Statement() {
            @Override
            public void evaluate() throws Throwable {
                log.info(logToWrite);
                String actual = outputCapture.toString();
                actual = actual.substring(actual.indexOf(loggerName) + loggerName.length()).trim();
                assertEquals(expectLog, actual);
            }
        }, Description.EMPTY).evaluate();
    }
}
