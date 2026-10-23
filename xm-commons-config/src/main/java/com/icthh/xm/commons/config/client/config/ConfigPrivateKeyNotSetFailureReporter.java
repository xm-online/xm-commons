package com.icthh.xm.commons.config.client.config;

import com.icthh.xm.commons.config.client.repository.ConfigPrivateKeyNotSetException;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.diagnostics.FailureAnalysisReporter;

/**
 * Prints {@link ConfigPrivateKeyNotSetFailureAnalyzer} to stderr with line breaks: the log keeps it in one line.
 */
public class ConfigPrivateKeyNotSetFailureReporter implements FailureAnalysisReporter {

    @Override
    public void report(FailureAnalysis analysis) {
        if (analysis.getCause() instanceof ConfigPrivateKeyNotSetException) {
            // one write, so the log lines of stdout do not get in the middle
            System.err.print(String.format("%n***************************%nAPPLICATION FAILED TO START"
                + "%n***************************%n%nDescription:%n%n%s%n%nAction:%n%n%s%n%n",
                analysis.getDescription(), analysis.getAction()));
            System.err.flush();
        }
    }
}
