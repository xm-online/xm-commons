package com.icthh.xm.commons.messaging.communication;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.icthh.xm.commons.messaging.communication.CommunicationConstants.LANGUAGE;
import static com.icthh.xm.commons.messaging.communication.CommunicationConstants.TEMPLATE_MODEL;
import static com.icthh.xm.commons.messaging.communication.CommunicationConstants.TEMPLATE_NAME;

@RequiredArgsConstructor
public class CommunicationCharacteristicsBuilder {

    private final ObjectMapper objectMapper;
    private final List<CommunicationRequestCharacteristic> characteristics = new ArrayList<>();

    @SneakyThrows
    public CommunicationCharacteristicsBuilder addTemplateModel(Map<String, Object> templateModel) {
        String convertedModel = objectMapper.writeValueAsString(templateModel);
        return addCharacteristic(TEMPLATE_MODEL, convertedModel);
    }

    public CommunicationCharacteristicsBuilder addLanguage(String language) {
        return addCharacteristic(LANGUAGE, language);
    }

    public CommunicationCharacteristicsBuilder addTemplateName(String templateName) {
        return addCharacteristic(TEMPLATE_NAME, templateName);
    }

    public CommunicationCharacteristicsBuilder addCharacteristic(String name, String value) {
        CommunicationRequestCharacteristic communicationRequestCharacteristic = new CommunicationRequestCharacteristic();
        communicationRequestCharacteristic.setName(name);
        communicationRequestCharacteristic.setValue(value);
        characteristics.add(communicationRequestCharacteristic);
        return this;
    }

    public List<CommunicationRequestCharacteristic> build() {
        return characteristics;
    }
}
