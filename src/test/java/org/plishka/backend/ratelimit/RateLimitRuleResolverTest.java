package org.plishka.backend.ratelimit;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.plishka.backend.security.AuthenticatedUserPrincipal;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimitRuleResolverTest {
    private final RateLimitRuleResolver resolver = new RateLimitRuleResolver();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resolve_ShouldUseEmailIdentityThenEmailIpThenIp_ForLoginPolicies() {
        MockHttpServletRequest request = request("POST", "/auth/login");
        request.setRemoteAddr("127.0.0.1");

        List<RateLimitKey> keys = resolver.resolve(request, Map.of("email", "User@Example.com"));

        assertEquals(List.of(
                new RateLimitKey(RateLimitPolicy.AUTH_LOGIN_EMAIL, "user@example.com"),
                new RateLimitKey(RateLimitPolicy.AUTH_LOGIN_EMAIL_IP, "user@example.com:127.0.0.1"),
                new RateLimitKey(RateLimitPolicy.AUTH_LOGIN_IP, "127.0.0.1")
        ), keys);
    }

    @Test
    void resolve_ShouldUseDeviceIpIdentityOrder_ForRefreshDeviceIpPolicy() {
        MockHttpServletRequest request = request("POST", "/auth/refresh");
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("Device-Id", "device-123");

        List<RateLimitKey> keys = resolver.resolve(request, Map.of());

        assertEquals(List.of(
                new RateLimitKey(RateLimitPolicy.AUTH_REFRESH_DEVICE_IP, "device-123:127.0.0.1"),
                new RateLimitKey(RateLimitPolicy.AUTH_REFRESH_IP, "127.0.0.1")
        ), keys);
    }

    @Test
    void resolve_ShouldNotRateLimitAdminEndpoint_WhenUserIsNotAdmin() {
        authenticate("ROLE_USER");

        List<RateLimitKey> keys = resolver.resolve(request("POST", "/admin/categories"), Map.of());

        assertTrue(keys.isEmpty());
    }

    @Test
    void resolve_ShouldRateLimitAdminWriteEndpoint_WhenUserIsAdmin() {
        authenticate("ROLE_ADMIN");

        List<RateLimitKey> keys = resolver.resolve(request("POST", "/admin/categories"), Map.of());

        assertEquals(List.of(new RateLimitKey(RateLimitPolicy.ADMIN_WRITE, "42")), keys);
    }

    @Test
    void resolve_ShouldRateLimitAdminPatchEndpoint_WhenUserIsAdmin() {
        authenticate("ROLE_ADMIN");

        List<RateLimitKey> keys = resolver.resolve(request("PATCH", "/admin/reviews/10/featured"), Map.of());

        assertEquals(List.of(new RateLimitKey(RateLimitPolicy.ADMIN_WRITE, "42")), keys);
    }

    @Test
    void resolve_ShouldNotApplyAuthenticatedReadLimitToAdminEndpoint_WhenUserIsNotAdmin() {
        authenticate("ROLE_USER");

        List<RateLimitKey> keys = resolver.resolve(request("GET", "/admin/categories"), Map.of());

        assertTrue(keys.isEmpty());
    }

    @Test
    void resolve_ShouldApplyAuthenticatedReadLimitToAdminEndpoint_WhenUserIsAdmin() {
        authenticate("ROLE_ADMIN");

        List<RateLimitKey> keys = resolver.resolve(request("GET", "/admin/categories"), Map.of());

        assertEquals(List.of(new RateLimitKey(RateLimitPolicy.AUTHENTICATED_READ, "42")), keys);
    }

    @Test
    void resolve_ShouldUseAuthenticatedReadForNewAdminReadEndpoints_WhenUserIsAdmin() {
        authenticate("ROLE_ADMIN");

        assertEquals(
                List.of(new RateLimitKey(RateLimitPolicy.AUTHENTICATED_READ, "42")),
                resolver.resolve(request("GET", "/admin/orders"), Map.of())
        );
        assertEquals(
                List.of(new RateLimitKey(RateLimitPolicy.AUTHENTICATED_READ, "42")),
                resolver.resolve(request("GET", "/admin/callback-requests"), Map.of())
        );
    }

    @Test
    void resolve_ShouldUseAdminWriteForNewAdminWrites_WhenUserIsAdmin() {
        authenticate("ROLE_ADMIN");

        assertEquals(
                List.of(new RateLimitKey(RateLimitPolicy.ADMIN_WRITE, "42")),
                resolver.resolve(request("PUT", "/admin/categories/order"), Map.of())
        );
        assertEquals(
                List.of(new RateLimitKey(RateLimitPolicy.ADMIN_WRITE, "42")),
                resolver.resolve(request("PUT", "/admin/settings"), Map.of())
        );
    }

    private static void authenticate(String role) {
        AuthenticatedUserPrincipal principal = AuthenticatedUserPrincipal.builder()
                .userId(42L)
                .email("user@example.com")
                .passwordHash("hash")
                .enabled(true)
                .accountNonLocked(true)
                .authorities(List.of(new SimpleGrantedAuthority(role)))
                .build();

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        ));
    }

    private static MockHttpServletRequest request(String method, String path) {
        return new MockHttpServletRequest(method, path);
    }
}
