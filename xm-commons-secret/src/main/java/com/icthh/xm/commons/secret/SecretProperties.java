package com.icthh.xm.commons.secret;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Settings under {@code application.secrets}. No toString on purpose: it would print the token.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "application.secrets")
public class SecretProperties {

    private boolean enabled = false;
    private String url;
    private String mount = "xm";
    /** {@code kubernetes} or {@code token}. */
    private String authentication = "kubernetes";
    private Kubernetes kubernetes = new Kubernetes();
    private String token;
    private Duration cacheTtl = Duration.ofSeconds(60);
    private List<String> lepReadBlacklist = new ArrayList<>();

    @Getter
    @Setter
    public static class Kubernetes {
        private String role;
        private String serviceAccountTokenPath = "/var/run/secrets/kubernetes.io/serviceaccount/token";
        private String path = "kubernetes";
    }
}
