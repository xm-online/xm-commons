package com.icthh.xm.commons.security.spring.config;

import com.icthh.xm.commons.security.XmPermissionCheckService;
import com.icthh.xm.commons.security.jwt.TokenProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = SecurityConfigurationManagementIntTest.TestConfiguration.class)
@TestPropertySource(properties = "jhipster.security.content-security-policy=default-src 'self'")
public class SecurityConfigurationManagementIntTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private XmPermissionCheckService permissionCheckService;

    private MockMvc mockMvc;

    @Before
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    public void testHealthIsPublic() throws Exception {
        mockMvc.perform(get("/management/health")).andExpect(status().isNotFound());
        mockMvc.perform(get("/management/health/liveness")).andExpect(status().isNotFound());
        mockMvc.perform(get("/management/health/readiness")).andExpect(status().isNotFound());
    }

    @Test
    public void testHealthDetailsAndInfoRequireSystemAdmin() throws Exception {
        mockMvc.perform(get("/management/health/db")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/management/info")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/management/info").with(role("SYSTEM-ADMIN"))).andExpect(status().isNotFound());
    }

    @Test
    public void testPrometheusIsClosedByDefault() throws Exception {
        mockMvc.perform(get("/management/prometheus")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/management/prometheus").with(role("SUPER-ADMIN"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/management/prometheus").with(role("SYSTEM-ADMIN"))).andExpect(status().isNotFound());
    }

    @Test
    public void testManagementRequiresSystemAdmin() throws Exception {
        mockMvc.perform(get("/management/env")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/management/env").with(role("SUPER-ADMIN"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/management/env").with(role("SYSTEM-ADMIN"))).andExpect(status().isNotFound());
    }

    @Test
    public void testApiDocsRequirePrivilege() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/v3/api-docs").with(role("SUPER-ADMIN"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/swagger-resources/configuration/ui")).andExpect(status().isUnauthorized());
    }

    @Test
    public void testLogsRequirePrivilege() throws Exception {
        mockMvc.perform(get("/management/logs").with(role("SYSTEM-ADMIN"))).andExpect(status().isNotFound());
        mockMvc.perform(put("/management/logs").with(role("SYSTEM-ADMIN"))).andExpect(status().isNotFound());
        mockMvc.perform(get("/management/logs").with(role("SUPER-ADMIN"))).andExpect(status().isForbidden());

        when(permissionCheckService.hasPermission("LOGS.GET")).thenReturn(true);
        mockMvc.perform(get("/management/logs").with(role("ROLE_USER"))).andExpect(status().isNotFound());
        mockMvc.perform(put("/management/logs").with(role("ROLE_USER"))).andExpect(status().isForbidden());

        when(permissionCheckService.hasPermission("LOGS.UPDATE")).thenReturn(true);
        mockMvc.perform(put("/management/logs").with(role("ROLE_USER"))).andExpect(status().isNotFound());
    }

    static RequestPostProcessor role(String role) {
        return authentication(new UsernamePasswordAuthenticationToken("user", null,
            List.of(new SimpleGrantedAuthority(role))));
    }

    @Configuration
    @EnableWebMvc
    @Import(SecurityConfiguration.class)
    static class TestConfiguration {

        @Bean
        TokenProvider tokenProvider() {
            return mock(TokenProvider.class);
        }

        @Bean
        XmPermissionCheckService permissionCheckService() {
            return mock(XmPermissionCheckService.class);
        }
    }
}
