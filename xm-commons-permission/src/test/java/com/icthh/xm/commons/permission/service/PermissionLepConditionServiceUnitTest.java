package com.icthh.xm.commons.permission.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.Test;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.security.access.AccessDeniedException;

public class PermissionLepConditionServiceUnitTest {

    private final PermissionLepService permissionLepService = mock(PermissionLepService.class);
    private final PermissionLepConditionService permissionLepConditionService =
        new PermissionLepConditionService(permissionLepService);
    private final SpelExpressionParser parser = new SpelExpressionParser();

    @Test
    public void shouldNotChangeConditionWithoutLepFilter() {
        String condition = "#returnObject.typeKey == 'COMPANY'";

        assertEquals(condition, applyFilter(condition));
        verifyNoInteractions(permissionLepService);
    }

    @Test
    public void shouldReplaceLepFilter() {
        when(permissionLepService.filter("PRIVILEGE", "ROLE_USER", Map.of())).thenReturn("a = 1");

        assertEquals("#returnObject.typeKey == 'COMPANY' && (a = 1)",
            applyFilter("#returnObject.typeKey == 'COMPANY' && #lepFilter"));
    }

    @Test(expected = AccessDeniedException.class)
    public void shouldDenyWhenLepFilterReturnsNull() {
        applyFilter("#lepFilter");
    }

    @Test
    public void shouldNotBindLepCheckWithoutVariable() {
        EvaluationContext context = bindCheck("#returnObject.typeKey == 'COMPANY'");

        assertNull(context.lookupVariable("lepCheck"));
        verifyNoInteractions(permissionLepService);
    }

    @Test
    public void shouldBindLepCheckResult() {
        when(permissionLepService.check(anyString(), anyString(), any())).thenReturn(false);

        EvaluationContext context = bindCheck("#lepCheck");

        assertFalse(parser.parseExpression("#lepCheck").getValue(context, Boolean.class));
    }

    private String applyFilter(String condition) {
        return permissionLepConditionService.applyFilter(condition, "PRIVILEGE", "ROLE_USER", Map.of());
    }

    private EvaluationContext bindCheck(String condition) {
        EvaluationContext context = new XmPermissionEvaluationContextBuilder().build(Map.of());
        permissionLepConditionService.bindCheck(context, "PRIVILEGE", "ROLE_USER", Map.of(),
            parser.parseExpression(condition));
        return context;
    }
}
