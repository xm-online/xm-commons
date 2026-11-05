package com.icthh.xm.commons.permission.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.permission.access.subject.Subject;
import java.util.Map;
import org.junit.Test;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.security.access.AccessDeniedException;

public class PermissionLepConditionServiceUnitTest {

    private static final String PRIVILEGE = "PRIVILEGE";
    private static final String ROLE = "ROLE_USER";

    private final Subject subject = new Subject("login", "user-key", ROLE);
    private final PermissionLepService permissionLepService = mock(PermissionLepService.class);
    private final PermissionLepConditionService permissionLepConditionService =
        new PermissionLepConditionService(permissionLepService, new XmPermissionEvaluationContextBuilder());
    private final SpelExpressionParser parser = new SpelExpressionParser();

    @Test
    public void shouldNotChangeConditionWithoutLepFilter() {
        String condition = "#returnObject.typeKey == 'COMPANY'";

        assertEquals(condition, applyFilter(condition, Map.of()));
        verifyZeroInteractions(permissionLepService);
    }

    @Test
    public void shouldNotChangeLepFilterInsideStringLiteral() {
        String condition = "#returnObject.name == '#lep.filter(''company'')'";

        assertEquals(condition, applyFilter(condition, Map.of()));
        verifyZeroInteractions(permissionLepService);
    }

    @Test
    public void shouldReplaceLepFilterWithoutParams() {
        when(permissionLepService.filter("company", PRIVILEGE, ROLE, Map.of(), Map.of("subject", subject)))
            .thenReturn("a = 1");

        assertEquals("#returnObject.typeKey == 'COMPANY' && (a = 1)",
            applyFilter("#returnObject.typeKey == 'COMPANY' && #lep.filter('company')", Map.of()));
    }

    @Test
    public void shouldEvaluateLepFilterParamsAndPassContextParams() {
        Map<String, Object> contextParams = Map.of("query", "name:a*", "entityClass", String.class);
        when(permissionLepService.filter("company", PRIVILEGE, ROLE,
            Map.of("typeKey", "COMPANY", "userKey", "user-key", "label", "it's (a) test"),
            Map.of("query", "name:a*", "entityClass", String.class, "subject", subject)))
            .thenReturn("a = 1");

        assertEquals("#returnObject.id > 0 && (a = 1) || #returnObject.id < 0",
            applyFilter("#returnObject.id > 0 && #lep.filter( 'company' , "
                + "{typeKey: 'COMPANY', userKey: #subject.userKey, label: 'it''s (a) test'} ) || #returnObject.id < 0",
                contextParams));
    }

    @Test
    public void shouldReplaceEveryLepFilter() {
        when(permissionLepService.filter("first", PRIVILEGE, ROLE, Map.of(), Map.of("subject", subject)))
            .thenReturn("a = 1");
        when(permissionLepService.filter("second", PRIVILEGE, ROLE, Map.of("n", 2), Map.of("subject", subject)))
            .thenReturn("b = 2");

        assertEquals("(a = 1) || ((b = 2) && #returnObject.id > 0)",
            applyFilter("#lep.filter('first') || (#lep.filter('second', {n: 2}) && #returnObject.id > 0)",
                Map.of()));
    }

    @Test(expected = AccessDeniedException.class)
    public void shouldDenyWhenLepFilterReturnsNull() {
        applyFilter("#lep.filter('company')", Map.of());
    }

    @Test(expected = AccessDeniedException.class)
    public void shouldDenyWhenLepFilterReturnsBlank() {
        when(permissionLepService.filter("company", PRIVILEGE, ROLE, Map.of(), Map.of("subject", subject)))
            .thenReturn("   ");

        applyFilter("#lep.filter('company')", Map.of());
    }

    @Test
    public void shouldCallLepCheckOnlyWhenEvaluated() {
        Map<String, Object> resources = Map.of("returnObject", Map.of("typeKey", "PERSON"));
        EvaluationContext context = bind(resources);

        assertFalse(evaluate("#returnObject['typeKey'] == 'COMPANY' && #lep.check('company')", context));
        verifyZeroInteractions(permissionLepService);
    }

    @Test
    public void shouldEvaluateLepCheckWithParams() {
        Map<String, Object> resources = Map.of("returnObject", Map.of("typeKey", "COMPANY"));
        when(permissionLepService.check("company", PRIVILEGE, ROLE, Map.of("typeKey", "COMPANY"), resources))
            .thenReturn(true);

        assertTrue(evaluate("#lep.check('company', {typeKey: #returnObject['typeKey']})", bind(resources)));
    }

    @Test
    public void shouldDenyWhenLepCheckReturnsFalse() {
        assertFalse(evaluate("#lep.check('company')", bind(Map.of())));
    }

    private String applyFilter(String condition, Map<String, Object> contextParams) {
        return permissionLepConditionService.applyFilter(condition, PRIVILEGE, ROLE, subject, contextParams);
    }

    private EvaluationContext bind(Map<String, Object> resources) {
        EvaluationContext context = new XmPermissionEvaluationContextBuilder().build(resources);
        permissionLepConditionService.bind(context, PRIVILEGE, ROLE, resources);
        return context;
    }

    private boolean evaluate(String condition, EvaluationContext context) {
        return parser.parseExpression(condition).getValue(context, Boolean.class);
    }
}
