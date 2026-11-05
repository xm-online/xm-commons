package com.icthh.xm.commons.config.client.applied;

import com.icthh.xm.commons.security.XmPermissionCheckService;
import com.icthh.xm.commons.security.spring.config.SecurityConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import tools.jackson.databind.JsonNode;

import static com.icthh.xm.commons.config.client.applied.AppliedConfigTestSupport.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A service with its own filter chain: {@code /api/**} is only authenticated and the commons URL rule is absent,
 * like in xm-ms-entity. The handler must check the privilege itself.
 */
@RunWith(SpringRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = AppliedConfigResourcePlainChainIntTest.TestConfiguration.class)
public class AppliedConfigResourcePlainChainIntTest {

    private static final String URL = SecurityConfiguration.APPLIED_CONFIG_PATH;

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private XmPermissionCheckService permissionCheckService;

    private MockMvc mockMvc;

    @Before
    public void setup() {
        reset(permissionCheckService);
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    public void tenantTokenWithoutPrivilegeIsForbidden() throws Exception {
        mockMvc.perform(get(URL).with(user("XM", "ROLE_USER"))).andExpect(status().isForbidden());
    }

    @Test
    public void tenantTokenWithPrivilegeGetsItsTenantScope() throws Exception {
        when(permissionCheckService.hasPermission("APPLIED_CONFIG.GET")).thenReturn(true);

        JsonNode body = json(mockMvc.perform(get(URL).with(user("XM", "ROLE_USER"))).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString());

        assertThat(keys(body.get("hashes"))).containsExactly("XM", "commons");
        mockMvc.perform(get(URL).param("tenant", "OTHER").with(user("XM", "ROLE_USER")))
            .andExpect(status().isForbidden());
    }

    @Test
    public void managementRoleWithoutPrivilegeGetsEverything() throws Exception {
        JsonNode body = json(mockMvc.perform(get(URL).with(user("XM", "SYSTEM-ADMIN"))).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString());

        assertThat(keys(body.get("hashes"))).containsExactly("OTHER", "XM", "commons");
    }

    @Test
    public void noTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import(AppliedConfigConfiguration.AppliedConfigWebConfiguration.class)
    static class TestConfiguration {

        @Bean
        SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            return http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.requestMatchers("/api/**").authenticated())
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .build();
        }

        @Bean
        XmPermissionCheckService permissionCheckService() {
            return mock(XmPermissionCheckService.class);
        }

        @Bean
        AppliedConfigService appliedConfigService() {
            return seededService();
        }
    }
}
