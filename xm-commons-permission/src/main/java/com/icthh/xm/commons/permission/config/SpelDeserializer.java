package com.icthh.xm.commons.permission.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.io.IOException;

public class SpelDeserializer extends StdDeserializer<Expression> {

    private transient ExpressionParser parser = new SpelExpressionParser();

    public SpelDeserializer() {
        this(null);
    }

    protected SpelDeserializer(Class<?> dc) {
        super(dc);
    }

    @Override
    public Expression deserialize(JsonParser jp, DeserializationContext context) throws IOException {
        String expressionString = jp.getCodec().readValue(jp, String.class);
        return parser.parseExpression(expressionString);
    }
}
