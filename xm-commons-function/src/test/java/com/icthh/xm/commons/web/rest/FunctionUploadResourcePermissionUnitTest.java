package com.icthh.xm.commons.web.rest;

import com.icthh.xm.commons.domain.FunctionResult;
import com.icthh.xm.commons.service.FunctionServiceFacade;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.authorization.method.PreAuthorizeAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.web.servlet.HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE;
import static org.springframework.web.servlet.HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE;

/**
 * Catch-all upload handlers must check FUNCTION.UPLOAD.CALL against the real function key taken from the URL.
 */
class FunctionUploadResourcePermissionUnitTest {

    private final List<Object> checkedResources = new ArrayList<>();
    private final FunctionServiceFacade functionService = mock(FunctionServiceFacade.class);
    private FunctionUploadResource resource;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("user", "pass", "ROLE_USER"));
        when(functionService.execute(any(), any(), any())).thenReturn(mock(FunctionResult.class));
        resource = secured(new FunctionUploadResource(functionService));
        resource.setSelf(resource);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void legacyUploadChecksPermissionWithFunctionKey() {
        MockHttpServletRequest request = request("/api/functions/A/B/upload", "/api/functions/**");

        resource.callUploadFunction(request, List.of(), request);

        assertThat(checkedResources).containsExactly(Map.of("functionKey", "A/B"));
        verify(functionService).execute(eq("A/B"), any(), eq("POST"));
    }

    @Test
    void uploadCatchAllChecksPermissionOnceWithFunctionKey() {
        MockHttpServletRequest request = request("/api/functions/upload/functions/A/B", "/api/functions/upload/functions/**");

        resource.getCallUploadFunction(request, new MockMultipartFile("file", "f.txt", null, new byte[0]));

        assertThat(checkedResources).containsExactly(Map.of("functionKey", "A/B"));
        verify(functionService).execute(eq("A/B"), any(), eq("POST"));
    }

    private static MockHttpServletRequest request(String path, String pattern) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setAttribute(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, path);
        request.setAttribute(BEST_MATCHING_PATTERN_ATTRIBUTE, pattern);
        return request;
    }

    @SuppressWarnings("unchecked")
    private <T> T secured(T target) {
        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setPermissionEvaluator(new PermissionEvaluator() {
            @Override
            public boolean hasPermission(Authentication authentication, Object resource, Object permission) {
                checkedResources.add(resource);
                return true;
            }

            @Override
            public boolean hasPermission(Authentication authentication, Serializable id, String type, Object permission) {
                return false;
            }
        });
        PreAuthorizeAuthorizationManager authorizationManager = new PreAuthorizeAuthorizationManager();
        authorizationManager.setExpressionHandler(expressionHandler);

        ProxyFactory proxyFactory = new ProxyFactory(target);
        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAdvisor(AuthorizationManagerBeforeMethodInterceptor.preAuthorize(authorizationManager));
        return (T) proxyFactory.getProxy();
    }
}
