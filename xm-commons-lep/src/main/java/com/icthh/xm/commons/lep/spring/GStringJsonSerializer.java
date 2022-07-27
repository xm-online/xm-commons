package com.icthh.xm.commons.lep.spring;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import groovy.lang.GString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class GStringJsonSerializer extends StdSerializer<GString> {

    @Autowired
    public GStringJsonSerializer(ObjectMapper objectMapper) {
        super(GString.class);

        SimpleModule module = new SimpleModule();
        module.addSerializer(GString.class, this);
        objectMapper.registerModule(module);
    }

    @Override
    public void serialize(GString value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(value.toString());
    }

}
