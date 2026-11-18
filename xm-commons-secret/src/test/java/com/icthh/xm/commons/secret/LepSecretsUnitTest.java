package com.icthh.xm.commons.secret;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.icthh.xm.commons.config.client.service.TenantConfigService;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.lep.TargetProceedingLep;
import com.icthh.xm.commons.lep.api.LepBaseKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.icthh.xm.commons.secret.SecretTestSupport.provider;
import static com.icthh.xm.commons.secret.SecretTestSupport.tenantHolder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LepSecretsUnitTest {

    @Mock
    private SecretStore store;
    @Mock
    private TenantConfigService tenantConfigService;

    private SecretProperties properties;
    private SecretService service;
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private final Logger serviceLogger = (Logger) LoggerFactory.getLogger(SecretService.class);

    @BeforeEach
    void setUp() {
        properties = new SecretProperties();
        properties.setLepReadBlacklist(List.of("integrations/**"));
        service = new SecretService(tenantHolder("xm"), properties, provider(store));
        appender.start();
        serviceLogger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        serviceLogger.detachAppender(appender);
    }

    private LepSecrets lepSecrets() {
        return new LepSecrets(service, properties, Optional.of(tenantConfigService), "lep:g:k");
    }

    @Test
    void servicePatternBlocksReadButNotPutAndDelete() {
        when(tenantConfigService.getConfig()).thenReturn(Map.of());
        LepSecrets secrets = lepSecrets();

        assertThatThrownBy(() -> secrets.get("integrations/x/y")).isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("code", SecretService.ERROR_READ_DENIED);
        assertThat(secrets.put("integrations/x/y", Map.of("k", "v"))).isEqualTo("secret:integrations/x/y");
        secrets.delete("integrations/x/y");
        verify(store).write("XM/integrations/x/y", Map.of("k", "v"));
        verify(store).delete("XM/integrations/x/y");
    }

    @Test
    void tenantPatternsAreAddedToServiceOnes() {
        when(tenantConfigService.getConfig())
            .thenReturn(Map.of("secrets", Map.of("lepReadBlacklist", List.of("crm/*"))));
        when(store.read("XM/other")).thenReturn(Optional.of(Map.of("k", "v")));
        LepSecrets secrets = lepSecrets();

        assertThatThrownBy(() -> secrets.get("crm/a")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> secrets.get("integrations/a")).isInstanceOf(BusinessException.class);
        assertThat(secrets.get("other")).containsEntry("k", "v");
    }

    @Test
    void javaGetIgnoresBlacklist() {
        when(store.read("XM/integrations/a")).thenReturn(Optional.of(Map.of("k", "v")));
        assertThat(service.get("integrations/a", "k")).isEqualTo("v");
    }

    @Test
    void tenantWithoutSecretsSectionKeepsServicePatterns() {
        when(tenantConfigService.getConfig()).thenReturn(Map.of("other", "x"));
        when(store.read("XM/a")).thenReturn(Optional.of(Map.of("k", "v")));
        assertThat(lepSecrets().get("a")).containsEntry("k", "v");
    }

    @Test
    void worksWithoutTenantConfigService() {
        when(store.read("XM/a")).thenReturn(Optional.of(Map.of("k", "v")));
        LepSecrets secrets = new LepSecrets(service, properties, Optional.empty(), "lep");
        assertThat(secrets.get("a")).containsEntry("k", "v");
    }

    @Test
    void malformedTenantBlacklistFailsClosed() {
        when(tenantConfigService.getConfig())
            .thenReturn(Map.of("secrets", Map.of("lepReadBlacklist", Map.of("a", "b"))));
        assertThatThrownBy(() -> lepSecrets().get("a")).isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("code", LepSecrets.ERROR_BLACKLIST_INVALID);
    }

    @Test
    void contextBindsLepKeyToAuditLine() {
        LepBaseKey key = mock(LepBaseKey.class);
        when(key.getGroup()).thenReturn("grp");
        when(key.getBaseKey()).thenReturn("Base");
        TargetProceedingLep lep = mock(TargetProceedingLep.class);
        when(lep.getLepBaseKey()).thenReturn(key);
        when(tenantConfigService.getConfig()).thenReturn(Map.of());
        when(store.read("XM/a")).thenReturn(Optional.of(Map.of("k", "v")));

        SecretLepContext context = new SecretLepContext(service, properties, Optional.of(tenantConfigService));
        context.additionalContextValue(null, null, lep).orElseThrow().get("a");

        assertThat(appender.list).anySatisfy(event -> assertThat(event.getFormattedMessage())
            .contains("operation=get").contains("tenant=XM").contains("alias=a")
            .contains("caller=lep:grp:Base").contains("result=OK"));
        assertThat(context.additionalContextKey()).isEqualTo("secrets");
    }

    @Test
    void deniedIsAudited() {
        when(tenantConfigService.getConfig()).thenReturn(Map.of());
        assertThatThrownBy(() -> lepSecrets().get("integrations/a")).isInstanceOf(BusinessException.class);
        assertThat(appender.list).anySatisfy(event -> assertThat(event.getFormattedMessage())
            .contains("alias=integrations/a").contains("result=DENIED").contains("caller=lep:g:k"));
    }
}
