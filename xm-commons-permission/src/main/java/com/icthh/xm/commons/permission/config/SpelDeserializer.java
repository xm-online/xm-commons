package com.icthh.xm.commons.permission.config;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

public class SpelDeserializer extends StdDeserializer<Expression> {

    private transient ExpressionParser parser = new SpelExpressionParser();

    public SpelDeserializer() {
        super(Expression.class);
    }

    protected SpelDeserializer(Class<?> dc) {
        super(dc);
    }

    @Override
    public Expression deserialize(JsonParser jp, DeserializationContext context) throws JacksonException {
        String expressionString = context.readValue(jp, String.class);
        return parser.parseExpression(expressionString);
    }
}
