package com.icthh.xm.commons.config.defaults;

import org.junit.Test;
import org.springframework.cloud.consul.ConsulClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConsulCheckFailureClientUnitTest {

    private static final String CHECK_ID = "service:mstemplate-1";

    private final ConsulClient delegate = mock(ConsulClient.class);

    private final ConsulClient client = new ConsulCheckFailureClient(delegate);

    @Test
    public void testPassedCheckIsReturned() {
        when(delegate.agentCheckPass(CHECK_ID, null, null)).thenReturn(ResponseEntity.ok().build());

        assertThat(client.agentCheckPass(CHECK_ID, null, null).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void testUnknownCheckThrowsClientError() {
        when(delegate.agentCheckPass(CHECK_ID, null, null)).thenReturn(ResponseEntity.notFound().build());

        assertThatThrownBy(() -> client.agentCheckPass(CHECK_ID, null, null))
            .isInstanceOf(HttpClientErrorException.class)
            .satisfies(e -> assertThat(((HttpClientErrorException) e).getStatusCode().value()).isEqualTo(404));
    }

    @Test
    public void testCheckWithoutTtlThrowsServerError() {
        when(delegate.agentCheckFail(CHECK_ID, null, null))
            .thenReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());

        assertThatThrownBy(() -> client.agentCheckFail(CHECK_ID, null, null))
            .isInstanceOf(HttpServerErrorException.class);
    }

    @Test
    public void testOtherCallsKeepTheirResponse() {
        when(delegate.getAgentServices()).thenReturn(ResponseEntity.notFound().build());

        assertThat(client.getAgentServices().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
