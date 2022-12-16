package com.icthh.xm.commons.messaging.communication.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.messaging.communication.CommunicationMessage;
import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.commons.topic.service.KafkaTemplateService;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class CommunicationServiceUnitTest {

    private static final String COMMUNICATION_KAFKA_TOPIC_PATTERN = "%s_communication_queue";
    private static final String TENANT = "TEST";

    private CommunicationService subject;
    private TenantContextHolder tenantContextHolder;
    private KafkaTemplateService kafkaTemplateService;
    private ObjectMapper mapper = new ObjectMapper();

    @Before
    public void setup() {
        tenantContextHolder = mock(TenantContextHolder.class);
        kafkaTemplateService = mock(KafkaTemplateService.class);
        mockTenant(TENANT);
        subject = new CommunicationService(kafkaTemplateService, tenantContextHolder, mapper);
        ReflectionTestUtils.setField(subject, "topicName", COMMUNICATION_KAFKA_TOPIC_PATTERN);
    }

    @Test
    public void sendEmailEvent() {
        String topicName = String.format(COMMUNICATION_KAFKA_TOPIC_PATTERN, TENANT);
        CommunicationMessage communicationMessage = createCommunicationMessage();
        String convertedExpectedMessage = convertToString(communicationMessage);

        subject.sendEmailEvent(communicationMessage);

        verify(kafkaTemplateService).send(eq(topicName), eq(convertedExpectedMessage));
        verifyNoMoreInteractions(kafkaTemplateService);
    }

    private void mockTenant(String tenant) {
        TenantContext tenantContext = mock(TenantContext.class);
        when(tenantContext.getTenantKey()).thenReturn(Optional.of(TenantKey.valueOf(tenant)));
        when(tenantContextHolder.getContext()).thenReturn(tenantContext);
        when(tenantContextHolder.getTenantKey()).thenReturn(tenant);
    }

    private CommunicationMessage createCommunicationMessage() {
        CommunicationMessage communicationMessage = new CommunicationMessage();
        communicationMessage.setSubject("Subject");
        communicationMessage.setContent("content");
        communicationMessage.setCharacteristic(new ArrayList<>());
        return communicationMessage;
    }

    @SneakyThrows
    private String convertToString(Object object) {
        return mapper.writeValueAsString(object);
    }
}
