package com.icthh.xm.commons.security.spring.config;

import com.icthh.xm.commons.security.jwt.JWTConfigurer;
import com.icthh.xm.commons.security.jwt.TokenProvider;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

import static com.icthh.xm.commons.security.RoleConstant.SUPER_ADMIN;

@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfiguration {

    private final String contentSecurityPolicy;

    private final TokenProvider tokenProvider;

    public SecurityConfiguration(
        TokenProvider tokenProvider,
        @Value("${jhipster.security.content-security-policy}")
        String contentSecurityPolicy
    ) {
        this.tokenProvider = tokenProvider;
        this.contentSecurityPolicy = contentSecurityPolicy;
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
                    .requestMatchers("/api/**").authenticated()
                    .requestMatchers("/api/admin/**").hasAuthority(SUPER_ADMIN)
                    .requestMatchers("/management/health").permitAll()
                    .requestMatchers("/management/info").permitAll()
                    .requestMatchers("/management/prometheus").permitAll()
                    .requestMatchers("/management/prometheus/**").permitAll()
                    .requestMatchers("/management/**").hasAuthority(SUPER_ADMIN)
                    .requestMatchers("/swagger-resources/configuration/ui").permitAll()
            );
        // @formatter:on
    }

    private JWTConfigurer securityConfigurerAdapter() {
        return new JWTConfigurer(tokenProvider);
    }
}
