package com.icthh.xm.commons.permission.config;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;
import org.springframework.expression.Expression;

public class SpelSerializer extends StdSerializer<Expression> {

    public SpelSerializer() {
        this(Expression.class);
    }

    public SpelSerializer(Class<Expression> sc) {
        super(sc);
    }

    @Override
    public void serialize(Expression value, JsonGenerator gen, SerializationContext provider) throws JacksonException {
        gen.writeString(value.getExpressionString());
    }
}
