package com.icthh.xm.commons.config.defaults;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import org.springframework.cloud.consul.ConsulClient;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

/**
 * Spring Cloud Consul ignores 4xx and only logs 5xx answers of the agent (ConsulAutoConfiguration), so TtlScheduler
 * never sees that Consul does not know the service any more and heartbeat.reregister-service-on-failure does nothing.
 * <p>
 * The decorator turns a failed update of the TTL check into an exception again, as TtlScheduler expects.
 * Only these calls: consul config (ConsulPropertySources, ConfigWatch) expects 404 of a missing key as a response.
 * <p>
 * Remove when https://github.com/spring-cloud/spring-cloud-consul/issues/954 is fixed.
 */
@RequiredArgsConstructor
public class ConsulCheckFailureClient implements ConsulClient {

    @Delegate(excludes = TtlCheckCalls.class)
    private final ConsulClient delegate;

    @Override
    public ResponseEntity<Void> agentCheckPass(String checkId, String note, String aclToken) {
        return failOnError(delegate.agentCheckPass(checkId, note, aclToken));
    }

    @Override
    public ResponseEntity<Void> agentCheckWarn(String checkId, String note, String aclToken) {
        return failOnError(delegate.agentCheckWarn(checkId, note, aclToken));
    }

    @Override
    public ResponseEntity<Void> agentCheckFail(String checkId, String note, String aclToken) {
        return failOnError(delegate.agentCheckFail(checkId, note, aclToken));
    }

    private static ResponseEntity<Void> failOnError(ResponseEntity<Void> response) {
        HttpStatusCode status = response.getStatusCode();
        if (status.is4xxClientError()) {
            throw new HttpClientErrorException(status);
        }
        if (status.is5xxServerError()) {
            throw new HttpServerErrorException(status);
        }
        return response;
    }

    private interface TtlCheckCalls {

        ResponseEntity<Void> agentCheckPass(String checkId, String note, String aclToken);

        ResponseEntity<Void> agentCheckWarn(String checkId, String note, String aclToken);

        ResponseEntity<Void> agentCheckFail(String checkId, String note, String aclToken);
    }
}
