package com.icthh.xm.commons.secret;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.icthh.xm.commons.secret.SecretTestSupport.chain;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Runs the production construction path against a JDK HTTP server that plays Vault. */
class VaultSecretStoreUnitTest {

    private static final String VALUE = "never-in-messages";

    private record Request(String method, String path, String token, String body) {
    }

    private final List<Request> requests = new CopyOnWriteArrayList<>();
    private HttpServer vault;
    private VaultSecretStore store;

    @BeforeEach
    void startVault() throws IOException {
        vault = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        vault.createContext("/", this::handle);
        vault.start();
    }

    @AfterEach
    void stop() throws Exception {
        if (store != null) {
            store.destroy();
        }
        vault.stop(0);
    }

    private void handle(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        requests.add(new Request(method, path, exchange.getRequestHeaders().getFirst("X-Vault-Token"), body));
        int status = 200;
        String response = "{\"data\":{}}";
        if (path.equals("/v1/auth/kubernetes/login")) {
            response = "{\"auth\":{\"client_token\":\"login-token\",\"lease_duration\":3600,\"renewable\":false}}";
        } else if (path.endsWith("/data/XM/missing")) {
            status = 404;
            response = "{\"errors\":[]}";
        } else if (path.endsWith("/data/XM/broken")) {
            status = 500;
            response = "{\"errors\":[\"internal\"]}";
        } else if (method.equals("GET")) {
            response = "{\"data\":{\"data\":{\"k\":\"v\"},\"metadata\":{\"version\":1}}}";
        } else if (method.equals("DELETE")) {
            status = 204;
        }
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, status == 204 ? -1 : bytes.length);
        if (status != 204) {
            exchange.getResponseBody().write(bytes);
        }
        exchange.close();
    }

    private SecretProperties tokenProperties(String url) {
        SecretProperties properties = new SecretProperties();
        properties.setEnabled(true);
        properties.setAuthentication("token");
        properties.setToken("static-token");
        properties.setUrl(url);
        return properties;
    }

    private String url() {
        return "http://localhost:" + vault.getAddress().getPort();
    }

    @Test
    void kubernetesLoginThenKvReadUsesLoginToken(@TempDir Path dir) throws IOException {
        Path tokenFile = dir.resolve("token");
        Files.writeString(tokenFile, "jwt-1");
        SecretProperties properties = new SecretProperties();
        properties.setEnabled(true);
        properties.setUrl(url());
        properties.getKubernetes().setRole("r");
        properties.getKubernetes().setServiceAccountTokenPath(tokenFile.toString());
        store = new VaultSecretStore(properties);

        assertThat(store.read("XM/a")).contains(Map.of("k", "v"));

        Request login = requests.get(0);
        assertThat(login.method()).isEqualTo("POST");
        assertThat(login.path()).isEqualTo("/v1/auth/kubernetes/login");
        assertThat(login.body()).contains("\"role\":\"r\"").contains("\"jwt\":\"jwt-1\"");
        Request read = requests.get(1);
        assertThat(read.path()).isEqualTo("/v1/xm/data/XM/a");
        assertThat(read.token()).isEqualTo("login-token");
    }

    @Test
    void kvV2WriteSendsDataEnvelopeWithStaticTokenAndNoLogin() {
        store = new VaultSecretStore(tokenProperties(url()));
        store.write("XM/a", Map.of("k", "v"));

        assertThat(requests).hasSize(1);
        Request write = requests.get(0);
        assertThat(write.method()).isIn("POST", "PUT");
        assertThat(write.path()).isEqualTo("/v1/xm/data/XM/a");
        assertThat(write.body()).isEqualTo("{\"data\":{\"k\":\"v\"}}");
        assertThat(write.token()).isEqualTo("static-token");
    }

    @Test
    void kvV2ReadMissingIsEmpty() {
        store = new VaultSecretStore(tokenProperties(url()));
        assertThat(store.read("XM/a")).contains(Map.of("k", "v"));
        assertThat(store.read("XM/missing")).isEmpty();
    }

    @Test
    void kvV2DeleteTargetsDataPath() {
        store = new VaultSecretStore(tokenProperties(url()));
        store.delete("XM/a");

        assertThat(requests).hasSize(1);
        assertThat(requests.get(0).method()).isEqualTo("DELETE");
        assertThat(requests.get(0).path()).isEqualTo("/v1/xm/data/XM/a");
    }

    @Test
    void serverErrorBecomesStoreExceptionWithoutValue() {
        store = new VaultSecretStore(tokenProperties(url()));
        assertThatThrownBy(() -> store.write("XM/broken", Map.of("k", VALUE)))
            .isInstanceOf(SecretStoreException.class)
            .hasMessage("Secret store write failed")
            .satisfies(e -> assertThat(chain(e)).doesNotContain(VALUE));
    }

    @Test
    void unknownAuthenticationFailsNamingTheSetting() {
        SecretProperties properties = tokenProperties(url());
        properties.setAuthentication("Token");
        assertThatThrownBy(() -> new VaultSecretStore(properties))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("application.secrets.authentication");
    }

    @Test
    void unreachableVaultBecomesStoreException() {
        String closed = url();
        vault.stop(0);
        store = new VaultSecretStore(tokenProperties(closed));
        assertThatThrownBy(() -> store.write("XM/a", Map.of("k", VALUE)))
            .isInstanceOf(SecretStoreException.class)
            .satisfies(e -> assertThat(chain(e)).doesNotContain(VALUE));
    }
}
