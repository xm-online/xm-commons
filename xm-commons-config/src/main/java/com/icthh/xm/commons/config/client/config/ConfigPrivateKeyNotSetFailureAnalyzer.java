package com.icthh.xm.commons.config.client.config;

import com.icthh.xm.commons.config.client.repository.ConfigPrivateKeyNotSetException;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

public class ConfigPrivateKeyNotSetFailureAnalyzer extends AbstractFailureAnalyzer<ConfigPrivateKeyNotSetException> {

    private static final String DESCRIPTION = """
        application.config-private-key is not set: the microservice signs every request to /api/private/v2 \
        of xm-ms-config with this private key.""";

    private static final String ACTION = """
        1) Local development without keys: run with the profile migration \
        (xm-ms-config with the profile migration too), by one of:
        - gradlew: `./gradlew bootRun --args='--spring.profiles.active=dev,migration'`;
        - environment variable: `SPRING_PROFILES_INCLUDE=migration ./gradlew bootRun`;
        - jar or container: `SPRING_PROFILES_ACTIVE=prod,migration`.
        2) For production use: generate one key pair per environment with OpenSSL 3 \
        (macOS: `brew install openssl`, the system openssl has no ed25519):
        - `openssl genpkey -algorithm ed25519 -outform DER -out private.der`;
        - private key: `base64 < private.der | tr -d '\\n'`;
        - public key: `openssl pkey -inform DER -in private.der -pubout -outform DER | base64 | tr -d '\\n'`;
        - `rm private.der`.
        Other ways are in README of xm-ms-config, section "Keys for the private config API".
        Set the private key in every microservice by one of:
        - environment variable: `APPLICATION_CONFIG_PRIVATE_KEY=<private key>`;
        - gradlew: `./gradlew bootRun --args='--spring.profiles.active=dev --application.config-private-key=<private key>'`;
        - application-dev.yml: `config-private-key: <private key>` under `application:`.
        Set the public key in xm-ms-config: application.config-public-key (APPLICATION_CONFIG_PUBLIC_KEY).""";

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, ConfigPrivateKeyNotSetException cause) {
        return new FailureAnalysis(DESCRIPTION, ACTION, cause);
    }
}
