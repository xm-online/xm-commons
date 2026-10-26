package com.icthh.xm.commons.config.defaults;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.OperationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cloud.consul.discovery.ConsulDiscoveryProperties;
import org.springframework.cloud.consul.discovery.HeartbeatProperties;
import org.springframework.cloud.consul.discovery.TtlScheduler;
import org.springframework.cloud.consul.serviceregistry.ConsulAutoRegistration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;

@Slf4j
public class XmTtlScheduler extends TtlScheduler {

    private static final String CHECK_PREFIX = "service:";

    private final Map<String, ScheduledFuture<?>> heartbeats = new ConcurrentHashMap<>();
    private final TaskScheduler scheduler = new ConcurrentTaskScheduler(Executors.newSingleThreadScheduledExecutor());

    private final HeartbeatProperties heartbeatProperties;
    private final ConsulDiscoveryProperties discoveryProperties;
    private final ConsulClient client;
    private final ObjectProvider<HealthEndpoint> healthEndpoint;
    private final ObjectProvider<ConsulAutoRegistration> registration;
    private final boolean reregisterServiceOnFailure;

    public XmTtlScheduler(HeartbeatProperties heartbeatProperties, ConsulDiscoveryProperties discoveryProperties,
                          ConsulClient client, ObjectProvider<HealthEndpoint> healthEndpoint,
                          ObjectProvider<ConsulAutoRegistration> registration, boolean reregisterServiceOnFailure) {
        super(heartbeatProperties, client);
        this.heartbeatProperties = heartbeatProperties;
        this.discoveryProperties = discoveryProperties;
        this.client = client;
        this.healthEndpoint = healthEndpoint;
        this.registration = registration;
        this.reregisterServiceOnFailure = reregisterServiceOnFailure;
    }

    @Override
    public void add(String instanceId) {
        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(() -> heartbeat(instanceId),
            heartbeatInterval().toMillis());
        ScheduledFuture<?> previousTask = heartbeats.put(instanceId, task);
        if (previousTask != null) {
            previousTask.cancel(true);
        }
    }

    @Override
    public void remove(String instanceId) {
        ScheduledFuture<?> task = heartbeats.remove(instanceId);
        if (task != null) {
            task.cancel(true);
        }
    }

    Duration heartbeatInterval() {
        int ttl = heartbeatProperties.getTtlValue();
        double interval = Math.min(ttl - 1, Math.max(ttl * heartbeatProperties.getIntervalRatio(), 1));
        return Duration.ofMillis(Math.round(1000 * interval));
    }

    void heartbeat(String instanceId) {
        String checkId = instanceId.startsWith(CHECK_PREFIX) ? instanceId : CHECK_PREFIX + instanceId;
        try {
            sendStatus(checkId);
        } catch (OperationException e) {
            if (reregisterServiceOnFailure && isUnknownToConsul(e)) {
                reregister(instanceId);
            } else {
                log.warn("Consul heartbeat of {} failed: {}", checkId, e.getMessage());
            }
        } catch (RuntimeException e) {
            log.warn("Consul heartbeat of {} failed: {}", checkId, e.getMessage());
        }
    }

    private void sendStatus(String checkId) {
        Status status = healthStatus();
        if (Status.UP.equals(status)) {
            client.agentCheckPass(checkId, null, discoveryProperties.getAclToken());
        } else if (Status.DOWN.equals(status) || Status.OUT_OF_SERVICE.equals(status)) {
            client.agentCheckFail(checkId, null, discoveryProperties.getAclToken());
        }
    }

    private Status healthStatus() {
        HealthEndpoint endpoint = healthEndpoint.getIfAvailable();
        return endpoint == null ? Status.UP : endpoint.health().getStatus();
    }

    private static boolean isUnknownToConsul(OperationException e) {
        return e.getStatusCode() == 404 || e.getStatusCode() >= 500;
    }

    private void reregister(String instanceId) {
        if (!heartbeats.containsKey(instanceId)) {
            return;
        }
        ConsulAutoRegistration consulRegistration = registration.getIfAvailable();
        if (consulRegistration == null || !instanceId.equals(consulRegistration.getInstanceId())) {
            log.warn("Consul does not know the service {}, it is not registered again", instanceId);
            return;
        }
        try {
            log.info("Registering service with consul again: {}", consulRegistration.getService());
            client.agentServiceRegister(consulRegistration.getService(), discoveryProperties.getAclToken());
        } catch (RuntimeException e) {
            log.warn("Registering service {} with consul again failed: {}", instanceId, e.getMessage());
        }
    }
}
