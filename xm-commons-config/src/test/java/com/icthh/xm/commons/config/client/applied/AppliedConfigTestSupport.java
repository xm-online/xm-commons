package com.icthh.xm.commons.config.client.applied;

import com.icthh.xm.commons.config.client.api.FetchConfigurationSettings;
import com.icthh.xm.commons.config.client.repository.CommonConfigRepository;
import com.icthh.xm.commons.config.domain.Configuration;
import com.icthh.xm.commons.security.internal.XmAuthenticationDetails;
import io.jsonwebtoken.Jwts;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

final class AppliedConfigTestSupport {

    static final String XM_PATH = "/config/tenants/XM/entity/a.yml";
    static final String OTHER_PATH = "/config/tenants/OTHER/entity/b.yml";
    static final String COMMONS_PATH = "/config/tenants/commons/c.yml";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AppliedConfigTestSupport() {
    }

    /** A service holding configs of two tenants and commons, and a version with two external tenants. */
    static AppliedConfigService seededService() {
        AppliedConfigService service = new AppliedConfigService(mock(CommonConfigRepository.class),
            new FetchConfigurationSettings("entity", false), true);
        service.onInitialLoad(Map.of(
            XM_PATH, new Configuration(XM_PATH, "a"),
            OTHER_PATH, new Configuration(OTHER_PATH, "b"),
            COMMONS_PATH, new Configuration(COMMONS_PATH, "c")));
        service.recordEventVersion("{\"mainVersion\":\"m1\",\"externalTenantVersions\":"
            + "{\"XM\":{\"mainVersion\":\"x1\"},\"OTHER\":{\"mainVersion\":\"o1\"}}}");
        return service;
    }

    /** Authentication whose details carry the tenant, like a token authenticated by the JWT filter. */
    static RequestPostProcessor user(String tenant, String... authorities) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("user", null,
            java.util.Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList());
        auth.setDetails(new XmAuthenticationDetails(Jwts.claims().add("tenant", tenant).build(), "127.0.0.1", null, "t"));
        return authentication(auth);
    }

    static JsonNode json(String body) {
        return MAPPER.readTree(body);
    }

    static List<String> keys(JsonNode node) {
        return node.propertyNames().stream().sorted().toList();
    }
}
