package com.icthh.xm.commons.permission.service;

import com.icthh.xm.commons.permission.access.subject.Subject;
import lombok.RequiredArgsConstructor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.SpelNode;
import org.springframework.expression.spel.ast.CompoundExpression;
import org.springframework.expression.spel.ast.MethodReference;
import org.springframework.expression.spel.ast.VariableReference;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.valueOf;

@Service
@RequiredArgsConstructor
public class PermissionLepConditionService {

    private static final String LEP = "lep";
    private static final String LEP_VARIABLE = "#" + LEP;
    private static final String FILTER = "filter";
    private static final String SUBJECT = "subject";

    private final SpelExpressionParser parser = new SpelExpressionParser();

    private final PermissionLepService permissionLepService;
    private final PermissionEvaluationContextBuilder permissionEvaluationContextBuilder;

    public void bind(EvaluationContext context, Object privilege, String roleKey, Map<String, Object> contextParams) {
        context.setVariable(LEP, new PermissionLep(permissionLepService, valueOf(privilege), roleKey, contextParams));
    }

    public String applyFilter(String condition, Object privilege, String roleKey, Subject subject,
                              Map<String, Object> contextParams) {
        if (condition == null || !condition.contains(LEP_VARIABLE)) {
            return condition;
        }
        List<SpelNode> filterCalls = new ArrayList<>();
        collectFilterCalls(parser.parseRaw(condition).getAST(), filterCalls);
        if (filterCalls.isEmpty()) {
            return condition;
        }

        Map<String, Object> filterContextParams = new HashMap<>(contextParams);
        filterContextParams.put(SUBJECT, subject);
        EvaluationContext context = permissionEvaluationContextBuilder.build(Map.of(SUBJECT, subject));
        bind(context, privilege, roleKey, filterContextParams);

        filterCalls.sort(Comparator.comparingInt(SpelNode::getStartPosition).reversed());
        StringBuilder result = new StringBuilder(condition);
        for (SpelNode filterCall : filterCalls) {
            int start = filterCall.getStartPosition();
            int end = closingParenthesis(condition, filterCall.getChild(1).getEndPosition()) + 1;
            String filter = parser.parseExpression(condition.substring(start, end)).getValue(context, String.class);
            result.replace(start, end, "(" + filter + ")");
        }
        return result.toString();
    }

    private static void collectFilterCalls(SpelNode node, List<SpelNode> filterCalls) {
        if (isFilterCall(node)) {
            filterCalls.add(node);
            return;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            collectFilterCalls(node.getChild(i), filterCalls);
        }
    }

    private static boolean isFilterCall(SpelNode node) {
        if (!(node instanceof CompoundExpression) || node.getChildCount() != 2) {
            return false;
        }
        SpelNode variable = node.getChild(0);
        SpelNode method = node.getChild(1);
        return variable instanceof VariableReference
            && LEP_VARIABLE.equals(variable.toStringAST())
            && method instanceof MethodReference
            && FILTER.equals(((MethodReference) method).getName());
    }

    private static int closingParenthesis(String condition, int from) {
        int depth = 0;
        char quote = 0;
        for (int i = from; i < condition.length(); i++) {
            char c = condition.charAt(i);
            if (quote != 0) {
                quote = c == quote ? 0 : quote;
            } else if (c == '\'' || c == '"') {
                quote = c;
            } else if (c == '(') {
                depth++;
            } else if (c == ')' && --depth == 0) {
                return i;
            }
        }
        throw new IllegalArgumentException("Closing parenthesis of " + LEP_VARIABLE + "." + FILTER
            + " is not found in condition: " + condition);
    }
}
