package com.icthh.xm.commons.config.defaults;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.OperationException;
import com.ecwid.consul.v1.agent.model.NewService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.cloud.consul.discovery.ConsulDiscoveryProperties;
import org.springframework.cloud.consul.discovery.HeartbeatProperties;
import org.springframework.cloud.consul.serviceregistry.ConsulAutoRegistration;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class XmTtlSchedulerUnitTest {

    private static final String INSTANCE_ID = "uaa-8080";
    private static final String CHECK_ID = "service:" + INSTANCE_ID;
    private static final String ACL_TOKEN = "acl";

    private final HeartbeatProperties heartbeatProperties = new HeartbeatProperties();
    private final ConsulDiscoveryProperties discoveryProperties = mock(ConsulDiscoveryProperties.class);
    private final ConsulClient client = mock(ConsulClient.class);
    private final HealthEndpoint healthEndpoint = mock(HealthEndpoint.class);
    private final ConsulAutoRegistration registration = mock(ConsulAutoRegistration.class);
    private final NewService service = new NewService();

    @Before
    public void setUp() {
        when(discoveryProperties.getAclToken()).thenReturn(ACL_TOKEN);
        when(registration.getInstanceId()).thenReturn(INSTANCE_ID);
        when(registration.getService()).thenReturn(service);
    }

    @Test
    public void testUpPassesCheck() {
        when(healthEndpoint.health()).thenReturn(Health.up().build());

        scheduler(true).heartbeat(INSTANCE_ID);

        verify(client).agentCheckPass(eq(CHECK_ID), isNull(), eq(ACL_TOKEN));
    }

    @Test
    public void testDownFailsCheck() {
        when(healthEndpoint.health()).thenReturn(Health.down().build());

        scheduler(true).heartbeat(INSTANCE_ID);

        verify(client).agentCheckFail(eq(CHECK_ID), isNull(), eq(ACL_TOKEN));
    }

    @Test
    public void testOutOfServiceFailsCheck() {
        when(healthEndpoint.health()).thenReturn(Health.outOfService().build());

        scheduler(true).heartbeat(INSTANCE_ID);

        verify(client).agentCheckFail(eq(CHECK_ID), isNull(), eq(ACL_TOKEN));
    }

    @Test
    public void testUnknownStatusSendsNothing() {
        when(healthEndpoint.health()).thenReturn(Health.unknown().build());

        scheduler(true).heartbeat(INSTANCE_ID);

        verify(client, never()).agentCheckPass(anyString(), any(), any());
        verify(client, never()).agentCheckFail(anyString(), any(), any());
    }

    @Test
    public void testServiceIsRegisteredAgainWhenConsulDoesNotKnowIt() {
        when(healthEndpoint.health()).thenReturn(Health.up().build());
        when(client.agentCheckPass(eq(CHECK_ID), isNull(), eq(ACL_TOKEN)))
            .thenThrow(new OperationException(404, "Not Found", "Unknown check"));

        heartbeatOfAddedInstance(true);

        verify(client, atLeastOnce()).agentServiceRegister(service, ACL_TOKEN);
    }

    @Test
    public void testServiceIsRegisteredAgainOnServerError() {
        when(healthEndpoint.health()).thenReturn(Health.down().build());
        when(client.agentCheckFail(eq(CHECK_ID), isNull(), eq(ACL_TOKEN)))
            .thenThrow(new OperationException(500, "Internal Server Error", "does not have associated TTL"));

        heartbeatOfAddedInstance(true);

        verify(client, atLeastOnce()).agentServiceRegister(service, ACL_TOKEN);
    }

    @Test
    public void testServiceIsNotRegisteredAgainWhenSwitchedOff() {
        when(healthEndpoint.health()).thenReturn(Health.up().build());
        when(client.agentCheckPass(eq(CHECK_ID), isNull(), eq(ACL_TOKEN)))
            .thenThrow(new OperationException(404, "Not Found", "Unknown check"));

        heartbeatOfAddedInstance(false);

        verify(client, never()).agentServiceRegister(any(NewService.class), anyString());
    }

    @Test
    public void testServiceIsNotRegisteredAgainOnForbidden() {
        when(healthEndpoint.health()).thenReturn(Health.up().build());
        when(client.agentCheckPass(eq(CHECK_ID), isNull(), eq(ACL_TOKEN)))
            .thenThrow(new OperationException(403, "Forbidden", "ACL not found"));

        heartbeatOfAddedInstance(true);

        verify(client, never()).agentServiceRegister(any(NewService.class), anyString());
    }

    @Test
    public void testInstanceWithoutHeartbeatIsNotRegisteredAgain() {
        when(healthEndpoint.health()).thenReturn(Health.up().build());
        when(client.agentCheckPass(eq(CHECK_ID), isNull(), eq(ACL_TOKEN)))
            .thenThrow(new OperationException(404, "Not Found", "Unknown check"));

        scheduler(true).heartbeat(INSTANCE_ID);

        verify(client, never()).agentServiceRegister(any(NewService.class), anyString());
    }

    @Test
    public void testFailedHealthDoesNotStopHeartbeat() {
        when(healthEndpoint.health()).thenThrow(new IllegalStateException("health failed"));

        scheduler(true).heartbeat(INSTANCE_ID);

        verify(client, never()).agentCheckPass(anyString(), any(), any());
    }

    @Test
    public void testHeartbeatIntervalIsTwoThirdsOfTtl() {
        assertThat(scheduler(true).heartbeatInterval()).isEqualTo(Duration.ofSeconds(20));
    }

    private void heartbeatOfAddedInstance(boolean reregister) {
        XmTtlScheduler scheduler = scheduler(reregister);
        scheduler.add(INSTANCE_ID);
        try {
            scheduler.heartbeat(INSTANCE_ID);
        } finally {
            scheduler.remove(INSTANCE_ID);
        }
    }

    private XmTtlScheduler scheduler(boolean reregister) {
        return new XmTtlScheduler(heartbeatProperties, discoveryProperties, client,
            provider(HealthEndpoint.class, healthEndpoint), provider(ConsulAutoRegistration.class, registration),
            reregister);
    }

    private static <T> ObjectProvider<T> provider(Class<T> type, T bean) {
        return new StaticListableBeanFactory(Map.of(type.getSimpleName(), bean)).getBeanProvider(type);
    }
}
