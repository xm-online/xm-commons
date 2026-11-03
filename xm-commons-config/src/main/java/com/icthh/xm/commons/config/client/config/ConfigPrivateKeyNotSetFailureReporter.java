package com.icthh.xm.commons.config.client.config;

import com.icthh.xm.commons.config.client.repository.ConfigPrivateKeyNotSetException;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.diagnostics.FailureAnalysisReporter;

public class ConfigPrivateKeyNotSetFailureReporter implements FailureAnalysisReporter {

    @Override
    public void report(FailureAnalysis analysis) {
        if (analysis.getCause() instanceof ConfigPrivateKeyNotSetException) {
            System.err.print(String.format("%n***************************%nAPPLICATION FAILED TO START"
                + "%n***************************%n%nDescription:%n%n%s%n%nAction:%n%n%s%n%n",
                analysis.getDescription(), analysis.getAction()));
            System.err.flush();
        }
    }
}
