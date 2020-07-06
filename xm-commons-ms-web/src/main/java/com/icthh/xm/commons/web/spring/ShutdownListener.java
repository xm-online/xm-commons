package com.icthh.xm.commons.web.spring;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.agent.AgentConsulClient;
import com.ecwid.consul.v1.catalog.model.CatalogService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.discovery.event.InstancePreRegisteredEvent;
import org.springframework.cloud.client.serviceregistry.AbstractAutoServiceRegistration;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.ecwid.consul.v1.QueryParams.DEFAULT;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "gracefulShutdown.enabled", havingValue = "true")
public class ShutdownListener implements SmartLifecycle {

    private final AbstractAutoServiceRegistration autoServiceRegistration;
    private final ConsulClient consulClient;

    private Registration registration;

    @Value("${spring.application.name}")
    private String appName;

    @Value("${gracefulShutdown.shutdownDelay}")
    private Long shutdownDelay;

    @Override
    public void start() {

    }

    @EventListener(InstancePreRegisteredEvent.class)
    public void onRegister(InstancePreRegisteredEvent event) {
        registration = event.getRegistration();
    }

    @Override
    @SneakyThrows
    public void stop() {
        StopWatch time = StopWatch.createStarted();
        log.info("START: shutdown process");
        autoServiceRegistration.stop();
        if (this.registration != null) {
            String instanceId = this.registration.getInstanceId();
            Response<List<CatalogService>> response = consulClient.getCatalogService(appName, DEFAULT);
            response.getValue().stream()
                .filter(service -> service.getServiceId().equals(instanceId))
                .forEach(this::deregister);
        }
        Thread.sleep(shutdownDelay);
        log.info("STOP: shutdown process, {}ms", time.getTime());
    }

    private void deregister(CatalogService it) {
        String instanceId = this.registration.getInstanceId();
        StopWatch time = StopWatch.createStarted();
        log.info("start deregistration {}", appName);
        new AgentConsulClient(it.getAddress()).agentServiceDeregister(instanceId);
        log.info("stop deregistration {}, {}ms", appName, time.getTime());
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }
    @Override
    public boolean isAutoStartup() {
        return true;
    }
    @Override
    public boolean isRunning() {
        return true;
    }

}
