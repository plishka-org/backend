package org.plishka.backend.config;

import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.ratelimit.RateLimitFilter;
import org.plishka.backend.security.ActiveUserAuthorizationManager;
import org.plishka.backend.security.CustomAccessDeniedHandler;
import org.plishka.backend.security.CustomAuthenticationEntryPoint;
import org.plishka.backend.security.JwtAuthenticationFilter;
import org.plishka.backend.security.ShopModeAuthorizationManager;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationManagers;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final ActiveUserAuthorizationManager activeUserAuthorizationManager;
    private final ShopModeAuthorizationManager shopModeAuthorizationManager;
    private final RateLimitFilter rateLimitFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/actuator/health",
                                "/actuator/prometheus",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .requestMatchers(
                                "/cart",
                                "/cart/**"
                        ).access(activeUserAndShopModeEnabled())
                        .requestMatchers(
                                HttpMethod.POST,
                                "/orders",
                                "/users/me/orders/*/repeat"
                        ).access(activeUserAndShopModeEnabled())
                        .requestMatchers(
                                "/users/me",
                                "/users/me/**",
                                "/callback",
                                "/products/*/view"
                        ).access(activeUserAuthorizationManager)
                        .requestMatchers("/admin/**").access(AuthorizationManagers.allOf(
                                activeUserAuthorizationManager,
                                AuthorityAuthorizationManager.hasRole("ADMIN")
                        ))
                        .requestMatchers(
                                HttpMethod.POST,
                                "/auth/register",
                                "/auth/resend-verification",
                                "/auth/forgot-password",
                                "/auth/reset-password",
                                "/auth/verify-email-change",
                                "/auth/login",
                                "/auth/refresh"
                        ).permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/files/presign/download"
                        ).permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/auth/verify",
                                "/health",
                                "/version",
                                "/home",
                                "/about",
                                "/contacts-page",
                                "/categories",
                                "/products",
                                "/products/*",
                                "/products/*/related",
                                "/reviews",
                                "/reviews/*",
                                "/settings"
                        ).permitAll()
                        .anyRequest().access(activeUserAuthorizationManager)
                )
                .addFilterBefore(
                        jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(rateLimitFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private AuthorizationManager<RequestAuthorizationContext> activeUserAndShopModeEnabled() {
        return AuthorizationManagers.allOf(activeUserAuthorizationManager, shopModeAuthorizationManager);
    }

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilterRegistration(
            JwtAuthenticationFilter filter
    ) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.setEnabled(false);
        return registration;
    }
}
