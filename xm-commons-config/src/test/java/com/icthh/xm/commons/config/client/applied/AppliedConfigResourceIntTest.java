package com.icthh.xm.commons.config.client.applied;

import com.icthh.xm.commons.config.web.rest.AppliedConfigResource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import tools.jackson.databind.JsonNode;

import java.util.Map;

import static com.icthh.xm.commons.config.client.applied.AppliedConfigTestSupport.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The endpoint behind a plain filter chain ({@code /api/**} authenticated, no rule of its own, like in
 * xm-ms-entity): access is the {@code APPLIED_CONFIG.GET} permission, data scope is the service's rule.
 */
@RunWith(SpringRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = AppliedConfigResourceIntTest.TestConfiguration.class)
@TestPropertySource(properties = "xm-config.enabled=true")
public class AppliedConfigResourceIntTest {

    private static final String URL = "/api/applied-config";
    private static final String PRIVILEGE = "APPLIED_CONFIG.GET";

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private PermissionEvaluator permissionEvaluator;
    @Autowired
    private AppliedConfigService service;

    private MockMvc mockMvc;

    @Before
    public void setup() {
        reset(permissionEvaluator);
        when(permissionEvaluator.hasPermission(any(), any(), eq(PRIVILEGE))).thenReturn(true);
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private JsonNode body(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request)
        throws Exception {
        return json(mockMvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    @Test
    public void noTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
    }

    @Test
    public void tenantTokenSeesOnlyItsTenantAndCommons() throws Exception {
        JsonNode body = body(get(URL).with(user("XM", "ROLE_USER")));

        assertThat(keys(body.get("hashes"))).containsExactly("XM", "commons");
        assertThat(body.get("hashes").get("XM").get("configCount").asInt()).isEqualTo(1);
        assertThat(body.has("total")).isFalse();
        assertThat(body.has("configurationsHashSum")).isFalse();
        assertThat(body.get("fetchScope").get("fetchAll").asBoolean()).isFalse();
        JsonNode commit = body.get("commit");
        assertThat(commit.get("mainVersion").asString()).isEqualTo("m1");
        assertThat(keys(commit.get("externalTenantVersions"))).containsExactly("XM");
        assertThat(commit.has("raw")).isFalse();
        assertThat(commit.get("source").asString()).isEqualTo("event");
    }

    @Test
    public void tenantTokenAskingForAnotherTenantIsForbidden() throws Exception {
        mockMvc.perform(get(URL).param("tenant", "OTHER").with(user("XM", "ROLE_USER")))
            .andExpect(status().isForbidden());
    }

    @Test
    public void tenantParameterIsPassedToThePermissionCheck() throws Exception {
        mockMvc.perform(get(URL).param("tenant", "XM").with(user("XM", "ROLE_USER"))).andExpect(status().isOk());

        verify(permissionEvaluator).hasPermission(any(), eq(Map.of("tenant", "XM")), eq(PRIVILEGE));
    }

    @Test
    public void tenantTokenWithoutPrivilegeIsForbidden() throws Exception {
        when(permissionEvaluator.hasPermission(any(), any(), eq(PRIVILEGE))).thenReturn(false);

        mockMvc.perform(get(URL).with(user("XM", "ROLE_USER"))).andExpect(status().isForbidden());
    }

    @Test
    public void managementRoleSeesEverything() throws Exception {
        JsonNode body = body(get(URL).with(user("XM", "SYSTEM-ADMIN")));

        assertThat(keys(body.get("hashes"))).containsExactly("OTHER", "XM", "commons");
        assertThat(body.get("total").get("configCount").asInt()).isEqualTo(3);
        assertThat(keys(body.get("commit").get("externalTenantVersions"))).containsExactly("OTHER", "XM");
        assertThat(body.get("commit").has("raw")).isTrue();
    }

    @Test
    public void managementRoleCanAskForOneTenant() throws Exception {
        JsonNode body = body(get(URL).param("tenant", "other").with(user("XM", "SYSTEM-ADMIN")));

        assertThat(keys(body.get("hashes"))).containsExactly("OTHER", "commons");
        assertThat(body.has("total")).isTrue();
    }

    @Test
    public void detailsListIsLimitedToScopeAndMatchesAggregate() throws Exception {
        JsonNode body = body(get(URL).param("details", "true").with(user("XM", "ROLE_USER")));

        java.util.List<AppliedConfigService.PathHash> list = new java.util.ArrayList<>();
        body.get("configurationsHashSum").forEach(n ->
            list.add(new AppliedConfigService.PathHash(n.get("path").asString(), n.get("hashSum").asString())));
        assertThat(list).extracting(AppliedConfigService.PathHash::path).containsExactly(XM_PATH, COMMONS_PATH);
        assertThat(AppliedConfigService.aggregateOf(AppliedConfigService.filterByKey(list, "XM")).aggregate())
            .isEqualTo(body.get("hashes").get("XM").get("aggregate").asString());
        assertThat(service.getPathHashes()).hasSize(3);
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity
    @Import(AppliedConfigResource.class)
    static class TestConfiguration {

        @Bean
        SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            return http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.requestMatchers("/api/**").authenticated())
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .build();
        }

        @Bean
        PermissionEvaluator permissionEvaluator() {
            return mock(PermissionEvaluator.class);
        }

        @Bean
        static MethodSecurityExpressionHandler methodSecurityExpressionHandler(PermissionEvaluator evaluator) {
            DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
            handler.setPermissionEvaluator(evaluator);
            return handler;
        }

        @Bean
        AppliedConfigService appliedConfigService() {
            return seededService();
        }
    }
}
