package com.icthh.xm.commons.permission.service;

import lombok.RequiredArgsConstructor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.lang.String.valueOf;

@Service
@RequiredArgsConstructor
public class PermissionLepConditionService {

    private static final String CHECK = "lepCheck";
    private static final String FILTER = "lepFilter";

    private final PermissionLepService permissionLepService;

    public void bindCheck(EvaluationContext context, Object privilege, String roleKey, Map<String, Object> resources,
                          Expression... expressions) {
        boolean used = Stream.of(expressions)
            .filter(Objects::nonNull)
            .map(Expression::getExpressionString)
            .anyMatch(spel -> contains(spel, CHECK));
        if (used) {
            context.setVariable(CHECK, permissionLepService.check(valueOf(privilege), roleKey, resources));
        }
    }

    public String applyFilter(String spel, Object privilege, String roleKey, Map<String, Object> resources) {
        if (!contains(spel, FILTER)) {
            return spel;
        }
        String filter = permissionLepService.filter(valueOf(privilege), roleKey, resources);
        if (filter == null) {
            throw new AccessDeniedException("Access is denied");
        }
        return spel.replace("#" + FILTER, format("(%s)", filter));
    }

    private static boolean contains(String spel, String variable) {
        return spel != null && spel.contains("#" + variable);
    }
}
