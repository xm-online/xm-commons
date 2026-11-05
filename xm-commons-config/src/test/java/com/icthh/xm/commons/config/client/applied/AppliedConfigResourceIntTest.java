package com.icthh.xm.commons.config.client.applied;

import com.icthh.xm.commons.security.XmPermissionCheckService;
import com.icthh.xm.commons.security.jwt.TokenProvider;
import com.icthh.xm.commons.security.spring.config.SecurityConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
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

/** The endpoint behind the commons {@link SecurityConfiguration} (URL rule plus handler check). */
@RunWith(SpringRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = AppliedConfigResourceIntTest.TestConfiguration.class)
@TestPropertySource(properties = "jhipster.security.content-security-policy=default-src 'self'")
public class AppliedConfigResourceIntTest {

    private static final String URL = SecurityConfiguration.APPLIED_CONFIG_PATH;

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private XmPermissionCheckService permissionCheckService;
    @Autowired
    private AppliedConfigService service;

    private MockMvc mockMvc;

    @Before
    public void setup() {
        reset(permissionCheckService);
        when(permissionCheckService.hasPermission(SecurityConfiguration.APPLIED_CONFIG_PRIVILEGE)).thenReturn(true);
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
    public void tenantTokenWithoutPrivilegeIsForbidden() throws Exception {
        when(permissionCheckService.hasPermission(SecurityConfiguration.APPLIED_CONFIG_PRIVILEGE)).thenReturn(false);

        mockMvc.perform(get(URL).with(user("XM", "ROLE_USER"))).andExpect(status().isForbidden());
    }

    @Test
    public void managementRoleSeesEverythingWithoutPrivilege() throws Exception {
        when(permissionCheckService.hasPermission(SecurityConfiguration.APPLIED_CONFIG_PRIVILEGE)).thenReturn(false);

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
    @Import({SecurityConfiguration.class, AppliedConfigConfiguration.AppliedConfigWebConfiguration.class})
    static class TestConfiguration {

        @Bean
        TokenProvider tokenProvider() {
            return mock(TokenProvider.class);
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
