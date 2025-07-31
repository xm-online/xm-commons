package com.icthh.xm.commons.lep.spring;

import static org.springframework.expression.spel.SpelMessage.PROPERTY_OR_FIELD_NOT_READABLE;

import java.util.Map;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.ReflectiveMethodResolver;
import org.springframework.expression.spel.support.ReflectivePropertyAccessor;
import org.springframework.expression.spel.support.StandardEvaluationContext;

public class LepSpelReader {
    public static Object readFieldByPath(String spel, Map<String, Object> methodArguments) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.addPropertyAccessor(new MapAccessor());
        context.addPropertyAccessor(new ReflectivePropertyAccessor(false));
        context.addMethodResolver(new ReflectiveMethodResolver());
        methodArguments.forEach(context::setVariable);
        ExpressionParser parser = new SpelExpressionParser();
        try {
            return parser.parseExpression(spel).getValue(context, methodArguments);
        } catch (SpelEvaluationException ex) {
            if (ex.getMessageCode() == PROPERTY_OR_FIELD_NOT_READABLE) {
                return null;
            }
            throw ex;
        }
    }

}
