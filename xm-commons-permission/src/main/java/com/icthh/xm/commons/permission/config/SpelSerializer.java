package com.icthh.xm.commons.permission.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.springframework.expression.Expression;

import java.io.IOException;

public class SpelSerializer extends StdSerializer<Expression> {

    public SpelSerializer() {
        this(null);
    }

    public SpelSerializer(Class<Expression> sc) {
        super(sc);
    }

    @Override
    public void serialize(Expression value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeString(value.getExpressionString());
    }
}
