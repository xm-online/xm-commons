package com.icthh.xm.commons.config.defaults;

import org.junit.After;
import org.junit.Test;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthStatusHttpMapper;
import org.springframework.boot.actuate.health.ShowDetails;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.security.Principal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class AuthorityHealthWebEndpointResponseMapperUnitTest {

    private static final Health HEALTH = Health.up().withDetail("db", "UP").build();

    private final AuthorityHealthWebEndpointResponseMapper mapper = new AuthorityHealthWebEndpointResponseMapper(
        new HealthStatusHttpMapper(), ShowDetails.WHEN_AUTHORIZED, Set.of("SYSTEM-ADMIN"));

    @After
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void testAuthorityOfRoleSeesDetails() {
        authenticate("SYSTEM-ADMIN");

        assertThat(mapper.map(HEALTH, servletContext()).getBody().getDetails()).containsKey("db");
    }

    @Test
    public void testOtherAuthorityDoesNotSeeDetails() {
        authenticate("SUPER-ADMIN");

        assertThat(mapper.map(HEALTH, servletContext()).getBody().getDetails()).isEmpty();
    }

    @Test
    public void testDetailsEndpointOfComponentIsHiddenFromOtherAuthority() {
        authenticate("SUPER-ADMIN");

        assertThat(mapper.mapDetails(() -> HEALTH, servletContext()).getStatus()).isEqualTo(404);
    }

    @Test
    public void testDetailsEndpointOfComponentIsShownToAuthorityOfRole() {
        authenticate("SYSTEM-ADMIN");

        assertThat(mapper.mapDetails(() -> HEALTH, servletContext()).getStatus()).isEqualTo(200);
    }

    @Test
    public void testRolePrefixedAuthorityDoesNotSeeDetails() {
        authenticate("ROLE_SYSTEM-ADMIN");

        assertThat(mapper.map(HEALTH, servletContextWithRolePrefix()).getBody().getDetails()).isEmpty();
        assertThat(mapper.mapDetails(() -> HEALTH, servletContextWithRolePrefix()).getStatus()).isEqualTo(404);
    }

    private static SecurityContext servletContextWithRolePrefix() {
        return new SecurityContext() {

            @Override
            public Principal getPrincipal() {
                return SecurityContextHolder.getContext().getAuthentication();
            }

            @Override
            public boolean isUserInRole(String role) {
                return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
            }
        };
    }

    private static void authenticate(String authority) {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("user", null, authority));
    }

    private static SecurityContext servletContext() {
        return new SecurityContext() {

            @Override
            public Principal getPrincipal() {
                return SecurityContextHolder.getContext().getAuthentication();
            }

            @Override
            public boolean isUserInRole(String role) {
                return false;
            }
        };
    }
}
