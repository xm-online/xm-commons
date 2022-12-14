package com.icthh.xm.commons.mail.provider.communication;

import com.icthh.xm.commons.messaging.communication.CommunicationMessage;
import com.icthh.xm.commons.messaging.communication.CommunicationRequestCharacteristic;
import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.commons.topic.service.KafkaTemplateService;
import lombok.SneakyThrows;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class CommunicationMailServiceUnitTest {
    private static final String COMMUNICATION_KAFKA_TOPIC_PATTERN = "%s_communication_queue";
    private static final String TENANT = "TEST";
    private CommunicationMailService subject;

    private TenantContextHolder tenantContextHolder;
    private KafkaTemplateService kafkaTemplateService;

    @Before
    public void setup() {
        tenantContextHolder = mock(TenantContextHolder.class);
        kafkaTemplateService = mock(KafkaTemplateService.class);
        mockTenant(TENANT);
        subject = new CommunicationMailService(kafkaTemplateService, tenantContextHolder);
        ReflectionTestUtils.setField(subject, "topicName", COMMUNICATION_KAFKA_TOPIC_PATTERN);
    }

    @Test
    public void sendEmailEvent() {
        String topicName = String.format(COMMUNICATION_KAFKA_TOPIC_PATTERN, TENANT);
        CommunicationMessage communicationMessage = createCommunicationMessage(new ArrayList<>());
        TemplateModel templateModel = createTemplateModel();
        String convertedTemplateModel = convertToString(templateModel);
        List<CommunicationRequestCharacteristic> characteristics = List.of(createCharacteristic("templateModel", convertedTemplateModel));
        CommunicationMessage expectedMessage = createCommunicationMessage(characteristics);
        String convertedExpectedMessage = convertToString(expectedMessage);

        subject.sendEmailEvent(communicationMessage, templateModel);

        verify(kafkaTemplateService).send(eq(topicName), eq(convertedExpectedMessage));
        verifyNoMoreInteractions(kafkaTemplateService);
    }

    private void mockTenant(String tenant) {
        TenantContext tenantContext = mock(TenantContext.class);
        when(tenantContext.getTenantKey()).thenReturn(Optional.of(TenantKey.valueOf(tenant)));
        when(tenantContextHolder.getContext()).thenReturn(tenantContext);
        when(tenantContextHolder.getTenantKey()).thenReturn(tenant);
    }

    private CommunicationMessage createCommunicationMessage(List<CommunicationRequestCharacteristic> characteristics) {
        CommunicationMessage communicationMessage = new CommunicationMessage();
        communicationMessage.setSubject("Subject");
        communicationMessage.setContent("content");
        communicationMessage.setCharacteristic(characteristics);
        return communicationMessage;
    }

    private TemplateModel createTemplateModel() {
        TemplateModel templateModel = new TemplateModel();
        templateModel.setTemplateKey("templateKey");
        templateModel.setLangKey("en");
        return templateModel;
    }

    private CommunicationRequestCharacteristic createCharacteristic(String name, String value) {
        CommunicationRequestCharacteristic characteristic = new CommunicationRequestCharacteristic();
        characteristic.setName(name);
        characteristic.setValue(value);
        return characteristic;
    }

    @SneakyThrows
    private String convertToString(Object object) {
        return new ObjectMapper().writeValueAsString(object);
    }
}
