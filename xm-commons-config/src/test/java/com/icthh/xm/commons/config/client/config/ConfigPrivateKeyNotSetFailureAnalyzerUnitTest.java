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
    public void testExplainsMigrationProfileAndKeys() {
        FailureAnalysis analysis = analyze(new ConfigPrivateKeyNotSetException());

        assertThat(analysis.getDescription()).contains("application.config-private-key is not set");
        assertThat(analysis.getAction())
            .contains("`./gradlew bootRun --args='--spring.profiles.active=dev,migration'`")
            .contains("`SPRING_PROFILES_INCLUDE=migration ./gradlew bootRun`")
            .contains("For production use")
            .contains("`openssl genpkey -algorithm ed25519 -outform DER -out private.der`")
            .contains("`base64 < private.der | tr -d '\\n'`")
            .contains("`APPLICATION_CONFIG_PRIVATE_KEY=<private key>`")
            .contains("--application.config-private-key=<private key>")
            .contains("application-dev.yml: `config-private-key: <private key>` under `application:`");
    }

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
