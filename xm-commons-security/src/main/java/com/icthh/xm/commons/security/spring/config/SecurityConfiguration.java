package com.icthh.xm.commons.security.spring.config;

import com.icthh.xm.commons.security.XmPermissionCheckService;
import com.icthh.xm.commons.security.jwt.JWTConfigurer;
import com.icthh.xm.commons.security.jwt.TokenProvider;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

import java.util.HashSet;
import java.util.Set;

import static com.icthh.xm.commons.security.RoleConstant.ADMIN;
import static com.icthh.xm.commons.security.RoleConstant.SUPER_ADMIN;
import static com.icthh.xm.commons.security.RoleConstant.SYSTEM_ADMIN;
import static org.springframework.security.authorization.AuthorityAuthorizationManager.hasAnyAuthority;
import static org.springframework.security.authorization.AuthorityAuthorizationManager.hasAuthority;
import static org.springframework.security.authorization.SingleResultAuthorizationManager.permitAll;

@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfiguration {

    private static final String API_DOCS_PRIVILEGE = "API_DOCS.GET";

    private final String contentSecurityPolicy;

    private final TokenProvider tokenProvider;

    private final XmPermissionCheckService permissionCheckService;

    private final Set<String> accessPrivileges = new HashSet<>();

    @Value("${application.prometheus.public:false}")
    private boolean prometheusPublic;

    @Value("${application.management.info.public:false}")
    private boolean infoPublic;

    @Value("${application.api-docs.privilege-check:true}")
    private boolean apiDocsPrivilegeCheck = true;

    @Value("${application.management.access.role:" + SYSTEM_ADMIN + "}")
    private String managementRole = SYSTEM_ADMIN;

    public SecurityConfiguration(
        TokenProvider tokenProvider,
        @Value("${jhipster.security.content-security-policy}")
        String contentSecurityPolicy,
        XmPermissionCheckService permissionCheckService
    ) {
        this.tokenProvider = tokenProvider;
        this.contentSecurityPolicy = contentSecurityPolicy;
        this.permissionCheckService = permissionCheckService;
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring().requestMatchers("/h2-console/**");
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // @formatter:off
        http
            .csrf(AbstractHttpConfigurer::disable)
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives(contentSecurityPolicy))
                .referrerPolicy(referrer -> referrer
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                )
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
            )
            .sessionManagement((session) -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            );

        applyUrlSecurity(http);
        http.with(securityConfigurerAdapter(), Customizer.withDefaults());
        http.exceptionHandling(exceptionHandler ->
            exceptionHandler.authenticationEntryPoint(new UnauthorizedEntryPoint())
        );
        return http.build();
        // @formatter:on
    }

    @SneakyThrows
    protected HttpSecurity applyUrlSecurity(HttpSecurity http) {
        // @formatter:off
        return http
            .authorizeHttpRequests(auth ->
                auth.requestMatchers("/api/profile-info").permitAll()
                    .requestMatchers("/api/admin/**").hasAuthority(SUPER_ADMIN)
                    .requestMatchers("/api/**").authenticated()
                    .requestMatchers("/management/health").permitAll()
                    .requestMatchers("/management/health/liveness").permitAll()
                    .requestMatchers("/management/health/readiness").permitAll()
                    .requestMatchers("/management/prometheus").access(publicOrManagementAccess(prometheusPublic))
                    .requestMatchers("/management/prometheus/**").access(publicOrManagementAccess(prometheusPublic))
                    .requestMatchers("/management/info").access(publicOrManagementAccess(infoPublic))
                    .requestMatchers("/management/**").access(hasAuthority(managementRole))
                    .requestMatchers("/v3/api-docs/**").access(apiDocsAccess())
                    .requestMatchers("/swagger-resources/configuration/ui").access(swaggerUiConfigAccess())
            );
        // @formatter:on
    }

    protected AuthorizationManager<RequestAuthorizationContext> access(String privilege) {
        accessPrivileges.add(privilege);
        return (authentication, context) -> new AuthorizationDecision(permissionCheckService.hasPermission(privilege));
    }

    private AuthorizationManager<RequestAuthorizationContext> publicOrManagementAccess(boolean isPublic) {
        return isPublic ? permitAll() : hasAuthority(managementRole);
    }

    private AuthorizationManager<RequestAuthorizationContext> apiDocsAccess() {
        return apiDocsPrivilegeCheck ? access(API_DOCS_PRIVILEGE) : hasAnyAuthority(SUPER_ADMIN, ADMIN);
    }

    private AuthorizationManager<RequestAuthorizationContext> swaggerUiConfigAccess() {
        return apiDocsPrivilegeCheck ? access(API_DOCS_PRIVILEGE) : permitAll();
    }

    public Set<String> getAccessPrivileges() {
        return Set.copyOf(accessPrivileges);
    }

    private JWTConfigurer securityConfigurerAdapter() {
        return new JWTConfigurer(tokenProvider);
    }
}
