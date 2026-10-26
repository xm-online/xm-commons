package com.icthh.xm.commons.config.client.config;

import com.icthh.xm.commons.config.client.repository.ConfigPrivateKeyNotSetException;
import org.junit.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.diagnostics.FailureAnalysis;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfigPrivateKeyNotSetFailureAnalyzerUnitTest {

    @Test
    public void testReporterPrintsOnlyThisFailureToStderr() {
        assertThat(reportToStderr(analyze(new ConfigPrivateKeyNotSetException())))
            .contains("APPLICATION FAILED TO START")
            .contains("application.config-private-key is not set")
            .contains("\n- environment variable: `SPRING_PROFILES_INCLUDE=migration ./gradlew bootRun`;\n");
        assertThat(reportToStderr(new FailureAnalysis("other", "other", new IllegalStateException()))).isEmpty();
    }

    private static FailureAnalysis analyze(Throwable cause) {
        return new ConfigPrivateKeyNotSetFailureAnalyzer()
            .analyze(new BeanCreationException("configPrivateApi", "failed", cause));
    }

    private static String reportToStderr(FailureAnalysis analysis) {
        PrintStream stderr = System.err;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setErr(new PrintStream(out, true, StandardCharsets.UTF_8));
        try {
            new ConfigPrivateKeyNotSetFailureReporter().report(analysis);
        } finally {
            System.setErr(stderr);
        }
        return out.toString(StandardCharsets.UTF_8);
    }
}
