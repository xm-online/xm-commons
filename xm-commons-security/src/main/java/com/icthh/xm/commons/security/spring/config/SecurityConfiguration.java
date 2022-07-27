package com.icthh.xm.commons.security.spring.config;

import com.icthh.xm.commons.security.jwt.JWTConfigurer;
import com.icthh.xm.commons.security.jwt.TokenProvider;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

import static com.icthh.xm.commons.security.RoleConstant.SUPER_ADMIN;

@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
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
        return web -> web.ignoring().antMatchers("/h2-console/**");
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // @formatter:off
        http
            .csrf()
            .disable()
            .headers()
            .contentSecurityPolicy(contentSecurityPolicy)
        .and()
            .referrerPolicy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
        .and()
            .frameOptions()
            .deny()
        .and()
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        applyUrlSecurity(http);
        http.apply(securityConfigurerAdapter());
        http.exceptionHandling().authenticationEntryPoint(new UnauthorizedEntryPoint());
        return http.build();
        // @formatter:on
    }

    @SneakyThrows
    protected ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry applyUrlSecurity(HttpSecurity http) {
        // @formatter:off
        return http
                   .authorizeRequests()
                   .antMatchers("/api/profile-info").permitAll()
                   .antMatchers("/api/**").authenticated()
                   .antMatchers("/api/admin/**").hasAuthority(SUPER_ADMIN)
                   .antMatchers("/management/health").permitAll()
                   .antMatchers("/management/info").permitAll()
                   .antMatchers("/management/prometheus").permitAll()
                   .antMatchers("/management/prometheus/**").permitAll()
                   .antMatchers("/management/**").hasAuthority(SUPER_ADMIN)
                   .antMatchers("/swagger-resources/configuration/ui").permitAll();
        // @formatter:on
    }

    private JWTConfigurer securityConfigurerAdapter() {
        return new JWTConfigurer(tokenProvider);
    }
}
