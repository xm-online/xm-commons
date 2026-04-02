package com.icthh.xm.commons.lep.groovy.config;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdSerializer;
import groovy.lang.GString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GStringJsonSerializer extends StdSerializer<GString> {

    @Autowired
    public GStringJsonSerializer(JsonMapper jsonMapper) {
        super(GString.class);

        SimpleModule module = new SimpleModule();
        module.addSerializer(GString.class, this);
        jsonMapper.rebuild()
                .addModules(module)
                .build();
    }

    @Override
    public void serialize(GString value, JsonGenerator gen, SerializationContext provider) throws JacksonException {
        gen.writeString(value.toString());
    }
}
