package com.icthh.xm.commons.config.defaults;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthStatusHttpMapper;
import org.springframework.boot.actuate.health.HealthWebEndpointResponseMapper;
import org.springframework.boot.actuate.health.ShowDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.security.Principal;
import java.util.Set;
import java.util.function.Supplier;

public class AuthorityHealthWebEndpointResponseMapper extends HealthWebEndpointResponseMapper {

    public AuthorityHealthWebEndpointResponseMapper(HealthStatusHttpMapper statusHttpMapper, ShowDetails showDetails,
                                                    Set<String> authorizedRoles) {
        super(statusHttpMapper, showDetails, authorizedRoles);
    }

    @Override
    public WebEndpointResponse<Health> mapDetails(Supplier<Health> health, SecurityContext securityContext) {
        return super.mapDetails(health, new AuthoritySecurityContext(securityContext));
    }

    @Override
    public WebEndpointResponse<Health> map(Health health, SecurityContext securityContext, ShowDetails showDetails) {
        return super.map(health, new AuthoritySecurityContext(securityContext), showDetails);
    }

    @RequiredArgsConstructor
    static class AuthoritySecurityContext implements SecurityContext {

        private final SecurityContext delegate;

        @Override
        public Principal getPrincipal() {
            return delegate.getPrincipal();
        }

        @Override
        public boolean isUserInRole(String role) {
            return delegate.isUserInRole(role) || hasAuthority(role);
        }

        private static boolean hasAuthority(String role) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            return authentication != null && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role::equals);
        }
    }
}
