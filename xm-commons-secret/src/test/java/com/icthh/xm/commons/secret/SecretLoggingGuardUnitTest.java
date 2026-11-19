package com.icthh.xm.commons.secret;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.read.ListAppender;
import com.icthh.xm.commons.logging.aop.ServiceLoggingAspect;
import com.icthh.xm.commons.logging.config.LoggingConfigServiceStub;
import com.icthh.xm.commons.logging.util.BasePackageDetector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.icthh.xm.commons.secret.SecretTestSupport.chain;
import static com.icthh.xm.commons.secret.SecretTestSupport.provider;
import static com.icthh.xm.commons.secret.SecretTestSupport.tenantHolder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/** Guard: no log line of the secret path, aspect lines included, and no exception text holds a value. */
class SecretLoggingGuardUnitTest {

    private static final String VALUE = "s3cr3t-VALUE-42";

    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private final Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    private Level oldLevel;

    @BeforeEach
    void setUp() {
        oldLevel = root.getLevel();
        root.setLevel(Level.DEBUG);
        appender.start();
        root.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        root.detachAppender(appender);
        root.setLevel(oldLevel);
    }

    @Test
    void noValueInLogsOrExceptions() {
        SecretProperties properties = new SecretProperties();
        properties.setLepReadBlacklist(List.of("integrations/**"));
        SecretService target = new SecretService(tenantHolder("xm"), properties, provider(new FakeStore()));
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(new ServiceLoggingAspect(new LoggingConfigServiceStub(), mock(BasePackageDetector.class)));
        SecretService service = factory.getProxy();
        LepSecrets lep = new LepSecrets(service, properties.getLepReadBlacklist(),
            new TenantSecretsConfiguration(tenantHolder("xm")), "lep:g:k");

        List<Throwable> thrown = new ArrayList<>();
        service.put("ok", Map.of("f", VALUE));
        service.get("ok");
        service.get("ok", "f");
        service.resolve("secret:ok#f");
        service.delete("ok");
        run(thrown, () -> service.put("broken", Map.of("f", VALUE)));
        run(thrown, () -> service.get("missing"));
        run(thrown, () -> service.get("bad alias"));
        run(thrown, () -> service.get("ok", "nofield"));
        run(thrown, () -> lep.get("integrations/x"));

        assertThat(thrown).hasSize(5);
        thrown.forEach(t -> assertThat(chain(t)).doesNotContain(VALUE));
        assertThat(appender.list).isNotEmpty().allSatisfy(event -> {
            assertThat(event.getFormattedMessage()).doesNotContain(VALUE);
            assertThat(proxyChain(event.getThrowableProxy())).doesNotContain(VALUE);
        });
        assertThat(appender.list).filteredOn(e -> e.getFormattedMessage().startsWith("secret audit:")).hasSize(10);
    }

    private static void run(List<Throwable> thrown, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            thrown.add(e);
        }
    }

    private static String proxyChain(IThrowableProxy proxy) {
        StringBuilder text = new StringBuilder();
        for (IThrowableProxy p = proxy; p != null; p = p.getCause()) {
            text.append(p.getClassName()).append(": ").append(p.getMessage()).append('\n');
        }
        return text.toString();
    }

    private static final class FakeStore implements SecretStore {
        @Override
        public Optional<Map<String, String>> read(String path) {
            return path.endsWith("/missing") ? Optional.empty() : Optional.of(Map.of("f", VALUE));
        }

        @Override
        public void write(String path, Map<String, String> data) {
            if (path.endsWith("/broken")) {
                throw new SecretStoreException("Secret store write failed", new RuntimeException("status 500"));
            }
        }

        @Override
        public void delete(String path) {
            // nothing to remove
        }
    }
}
