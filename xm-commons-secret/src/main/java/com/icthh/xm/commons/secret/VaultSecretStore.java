package com.icthh.xm.commons.secret;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.KubernetesAuthentication;
import org.springframework.vault.authentication.KubernetesAuthenticationOptions;
import org.springframework.vault.authentication.KubernetesServiceAccountTokenFile;
import org.springframework.vault.authentication.LifecycleAwareSessionManager;
import org.springframework.vault.authentication.SessionManager;
import org.springframework.vault.authentication.SimpleSessionManager;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.ClientHttpRequestFactoryFactory;
import org.springframework.vault.client.RestTemplateBuilder;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.core.VaultKeyValueOperations;
import org.springframework.vault.core.VaultKeyValueOperationsSupport.KeyValueBackend;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.ClientOptions;
import org.springframework.vault.support.SslConfiguration;
import org.springframework.vault.support.VaultResponse;
import org.springframework.vault.VaultException;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Vault KV v2 store. The template and the scheduler are private on purpose: a TaskScheduler or VaultTemplate bean
 * would change {@code @Scheduled} and Spring Cloud Vault wiring in services.
 */
@Component
@ConditionalOnProperty(value = "application.secrets.enabled", havingValue = "true")
public class VaultSecretStore implements SecretStore, DisposableBean {

    private final SessionManager sessionManager;
    private final ThreadPoolTaskScheduler scheduler;
    private final VaultKeyValueOperations kv;

    public VaultSecretStore(SecretProperties properties) {
        VaultEndpoint endpoint = VaultEndpoint.from(URI.create(properties.getUrl()));
        String authentication = properties.getAuthentication();
        if (!"token".equals(authentication) && !"kubernetes".equals(authentication)) {
            throw new IllegalArgumentException(
                "application.secrets.authentication must be 'kubernetes' or 'token'");
        }
        ClientHttpRequestFactory requestFactory =
            ClientHttpRequestFactoryFactory.create(new ClientOptions(), SslConfiguration.unconfigured());
        if ("token".equals(authentication)) {
            this.scheduler = null;
            this.sessionManager = new SimpleSessionManager(new TokenAuthentication(properties.getToken()));
        } else {
            this.scheduler = new ThreadPoolTaskScheduler();
            this.scheduler.setThreadNamePrefix("secret-vault-");
            this.scheduler.initialize();
            RestTemplate loginClient = RestTemplateBuilder.builder().endpoint(endpoint).requestFactory(requestFactory).build();
            this.sessionManager = new LifecycleAwareSessionManager(
                kubernetesAuth(properties.getKubernetes(), loginClient), scheduler, loginClient);
        }
        this.kv = new VaultTemplate(endpoint, requestFactory, sessionManager)
            .opsForKeyValue(properties.getMount(), KeyValueBackend.KV_2);
    }

    private static ClientAuthentication kubernetesAuth(SecretProperties.Kubernetes k8s, RestTemplate loginClient) {
        KubernetesAuthenticationOptions options = KubernetesAuthenticationOptions.builder()
            .role(k8s.getRole())
            .path(k8s.getPath())
            .jwtSupplier(new KubernetesServiceAccountTokenFile(k8s.getServiceAccountTokenPath()))
            .build();
        return new KubernetesAuthentication(options, loginClient);
    }

    @Override
    public Optional<Map<String, String>> read(String path) {
        VaultResponse response = call("read", () -> kv.get(path));
        if (response == null || response.getData() == null) {
            return Optional.empty();
        }
        Map<String, String> data = new LinkedHashMap<>();
        response.getData().forEach((key, value) -> data.put(key, String.valueOf(value)));
        return Optional.of(data);
    }

    @Override
    public void write(String path, Map<String, String> data) {
        call("write", () -> {
            kv.put(path, data);
            return null;
        });
    }

    @Override
    public void delete(String path) {
        call("delete", () -> {
            kv.delete(path);
            return null;
        });
    }

    private <T> T call(String operation, Supplier<T> action) {
        try {
            return action.get();
        } catch (VaultException | RestClientException e) {
            throw new SecretStoreException("Secret store " + operation + " failed", e);
        }
    }

    @Override
    public void destroy() throws Exception {
        if (sessionManager instanceof DisposableBean disposable) {
            disposable.destroy();
        }
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }
}
