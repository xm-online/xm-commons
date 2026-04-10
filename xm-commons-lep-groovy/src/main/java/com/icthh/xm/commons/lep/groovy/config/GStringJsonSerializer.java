package com.icthh.xm.commons.lep.groovy.config;

import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;
import groovy.lang.GString;

@Component
public class GStringJsonSerializer extends StdSerializer<GString> {

    public GStringJsonSerializer() {
        super(GString.class);
    }

    @Override
    public void serialize(GString value, JsonGenerator gen, SerializationContext provider) throws JacksonException {
        gen.writeString(value.toString());
    }
}
