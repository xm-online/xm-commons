package com.icthh.xm.commons.security.spring.config;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static com.icthh.xm.commons.security.spring.config.SecurityConfigurationManagementIntTest.role;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The values of xm-defaults-migration.yml restore the rules before SYSTEM-ADMIN.
 */
@RunWith(SpringRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = SecurityConfigurationManagementIntTest.TestConfiguration.class)
@TestPropertySource(properties = {
    "jhipster.security.content-security-policy=default-src 'self'",
    "application.prometheus.public=true",
    "application.management.access.role=SUPER-ADMIN"
})
public class SecurityConfigurationManagementMigrationIntTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Before
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    public void testPrometheusIsPublic() throws Exception {
        mockMvc.perform(get("/management/prometheus")).andExpect(status().isNotFound());
        mockMvc.perform(get("/management/prometheus/scrape")).andExpect(status().isNotFound());
    }

    @Test
    public void testManagementRequiresSuperAdmin() throws Exception {
        mockMvc.perform(get("/management/env")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/management/env").with(role("SYSTEM-ADMIN"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/management/env").with(role("SUPER-ADMIN"))).andExpect(status().isNotFound());
    }
}
