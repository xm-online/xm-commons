package com.icthh.xm.commons.messaging.communication;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.util.Map;

import static com.icthh.xm.commons.messaging.communication.CommunicationConstants.LANGUAGE;
import static com.icthh.xm.commons.messaging.communication.CommunicationConstants.TEMPLATE_MODEL;
import static com.icthh.xm.commons.messaging.communication.CommunicationConstants.TEMPLATE_NAME;

@RequiredArgsConstructor
public class CommunicationMessageBuilder {

    private final CommunicationMessage communicationMessage;
    private final ObjectMapper objectMapper;

    @SneakyThrows
    public CommunicationMessageBuilder addTemplateModel(Map<String, Object> templateModel) {
        String convertedModel = objectMapper.writeValueAsString(templateModel);
        return addCharacteristic(TEMPLATE_MODEL, convertedModel);
    }

    public CommunicationMessageBuilder addLanguage(String language) {
        return addCharacteristic(LANGUAGE, language);
    }

    public CommunicationMessageBuilder addTemplateName(String templateName) {
        return addCharacteristic(TEMPLATE_NAME, templateName);
    }

    public CommunicationMessageBuilder addCharacteristic(String name, String value) {
        CommunicationRequestCharacteristic communicationRequestCharacteristic = new CommunicationRequestCharacteristic();
        communicationRequestCharacteristic.setName(name);
        communicationRequestCharacteristic.setValue(value);
        communicationMessage.getCharacteristic().add(communicationRequestCharacteristic);
        return this;
    }

    public CommunicationMessageBuilder addReceiverEmail(String email) {
        Receiver receiver = new Receiver();
        receiver.setEmail(email);
        communicationMessage.getReceiver().add(receiver);
        return this;
    }

    public CommunicationMessageBuilder addSenderId(String id) {
        Sender sender = new Sender();
        sender.setId(id);
        communicationMessage.setSender(sender);
        return this;
    }

    public CommunicationMessage build() {
        return communicationMessage;
    }
}
