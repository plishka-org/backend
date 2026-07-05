package org.plishka.backend.controller;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.plishka.backend.config.TimeConfig;
import org.plishka.backend.security.AuthenticatedUserPrincipal;
import org.plishka.backend.security.CustomUserDetailsService;
import org.plishka.backend.service.auth.JwtService;
import org.plishka.backend.service.settings.ShopModeService;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.mockito.Mockito.lenient;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

@Import(TimeConfig.class)
public abstract class BaseControllerTest {
    protected static final long ADMIN_ID = 1L;
    protected static final String ADMIN_EMAIL = "admin@example.com";

    @MockitoBean
    protected JwtService jwtService;

    @MockitoBean
    protected CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    protected ShopModeService shopModeService;

    @BeforeEach
    void allowShopModeByDefault() {
        lenient().when(shopModeService.isShopModeEnabled()).thenReturn(true);
    }

    protected static RequestPostProcessor authenticatedUser(Long userId, String email) {
        return authenticatedUser(userPrincipal(userId, email, true, true, "ROLE_USER"));
    }

    protected static RequestPostProcessor authenticatedAdmin() {
        return authenticatedUser(userPrincipal(ADMIN_ID, ADMIN_EMAIL, true, true, "ROLE_ADMIN"));
    }

    protected static RequestPostProcessor authenticatedUser(AuthenticatedUserPrincipal principal) {
        return authentication(new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        ));
    }

    protected static AuthenticatedUserPrincipal userPrincipal(
            Long userId,
            String email,
            boolean enabled,
            boolean accountNonLocked,
            String role
    ) {
        return AuthenticatedUserPrincipal.builder()
                .userId(userId)
                .email(email)
                .passwordHash("hash")
                .enabled(enabled)
                .accountNonLocked(accountNonLocked)
                .authorities(List.of(new SimpleGrantedAuthority(role)))
                .build();
    }
}
