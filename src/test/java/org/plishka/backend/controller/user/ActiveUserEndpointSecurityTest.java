package org.plishka.backend.controller.user;

import io.jsonwebtoken.Claims;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.plishka.backend.controller.BaseControllerTest;
import org.plishka.backend.security.AuthenticatedUserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ActiveUserEndpointSecurityTest extends BaseControllerTest {
    private static final Long USER_ID = 100L;
    private static final Long PRODUCT_ID = 10L;
    private static final Long MISSING_PRODUCT_ID = 999L;
    private static final String USER_EMAIL = "serhii@example.com";
    private static final String VALID_TOKEN = "valid-token";
    private static final String INVALID_TOKEN = "invalid-token";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_VALID_TOKEN = "Bearer " + VALID_TOKEN;
    private static final String DEVICE_ID = "device-1";
    private static final String IDEMPOTENCY_KEY = "11f2cbe7-3915-44f6-9bcd-3a1c70a47e92";
    private static final String PRODUCT_MEDIA_KEY =
            "products/10/images/2026/05/550e8400-e29b-41d4-a716-446655440000.jpg";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void activeUserEndpoints_ShouldReturn401_WhenUserIsAnonymous() throws Exception {
        for (MockHttpServletRequestBuilder request : activeUserEndpointRequests()) {
            mockMvc.perform(request)
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void activeUserEndpoints_ShouldReturn401_WhenTokenIsInvalid() throws Exception {
        when(jwtService.parseClaims(INVALID_TOKEN)).thenReturn(Optional.empty());

        mockMvc.perform(get("/users/me")
                        .header(AUTHORIZATION_HEADER, bearerToken(INVALID_TOKEN)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void activeUserEndpoints_ShouldReturn403_WhenUserIsBanned() throws Exception {
        mockJwtAuthentication(bannedUserPrincipal());

        assertActiveUserEndpointsAreForbidden();
    }

    @Test
    void activeUserEndpoints_ShouldReturn403_WhenEmailIsNotVerified() throws Exception {
        mockJwtAuthentication(unverifiedUserPrincipal());

        assertActiveUserEndpointsAreForbidden();
    }

    @Test
    void publicProductEndpoint_ShouldRemainAccessible_WhenJwtBelongsToBannedUser() throws Exception {
        mockJwtAuthentication(bannedUserPrincipal());

        mockMvc.perform(withValidToken(get("/products/{id}", MISSING_PRODUCT_ID)))
                .andExpect(status().isNotFound());
    }

    private static List<MockHttpServletRequestBuilder> activeUserEndpointRequests() {
        return List.of(
                get("/users/me"),
                put("/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Serhii",
                                  "phone": "+380501234567"
                                }
                                """),
                put("/users/me/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "newEmail": "new@example.com",
                                  "currentPassword": "OldPassword123"
                                }
                                """),
                put("/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "OldPassword123",
                                  "newPassword": "NewPassword123",
                                  "confirmPassword": "NewPassword123"
                                }
                                """),
                delete("/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "OldPassword123"
                                }
                                """),
                post("/auth/logout")
                        .header("Device-Id", DEVICE_ID),
                get("/users/me/favorites"),
                post("/users/me/favorites/{productId}", PRODUCT_ID),
                delete("/users/me/favorites/{productId}", PRODUCT_ID),
                post("/products/{id}/view", PRODUCT_ID),
                get("/users/me/viewed"),
                post("/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Serhii",
                                  "phone": "+380501234567",
                                  "message": "Please call me"
                                }
                                """),
                get("/users/me/callback-requests"),
                get("/cart"),
                post("/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": %d,
                                  "quantity": 1
                                }
                                """.formatted(PRODUCT_ID)),
                get("/users/me/orders"),
                post("/orders")
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerName": "Serhii",
                                  "deliveryCity": "Kyiv",
                                  "phone": "+380501234567",
                                  "notes": "Please call before delivery"
                                }
                                """),
                adminProductMediaAttachRequest()
        );
    }

    private static MockHttpServletRequestBuilder adminProductMediaAttachRequest() {
        return post("/admin/products/{id}/media/attach", PRODUCT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "s3Key": "%s"
                        }
                        """.formatted(PRODUCT_MEDIA_KEY));
    }

    private void mockJwtAuthentication(AuthenticatedUserPrincipal principal) {
        Claims claims = mock(Claims.class);
        when(jwtService.parseClaims(VALID_TOKEN)).thenReturn(Optional.of(claims));
        when(jwtService.extractEmail(claims)).thenReturn(principal.getUsername());
        when(customUserDetailsService.loadUserByUsername(principal.getUsername())).thenReturn(principal);
    }

    private void assertActiveUserEndpointsAreForbidden() throws Exception {
        for (MockHttpServletRequestBuilder request : activeUserEndpointRequests()) {
            mockMvc.perform(withValidToken(request))
                    .andExpect(status().isForbidden());
        }
    }

    private static MockHttpServletRequestBuilder withValidToken(MockHttpServletRequestBuilder request) {
        return request.header(AUTHORIZATION_HEADER, BEARER_VALID_TOKEN);
    }

    private static String bearerToken(String token) {
        return "Bearer " + token;
    }

    private static AuthenticatedUserPrincipal bannedUserPrincipal() {
        return userPrincipal(USER_ID, USER_EMAIL, true, false, "ROLE_USER");
    }

    private static AuthenticatedUserPrincipal unverifiedUserPrincipal() {
        return userPrincipal(USER_ID, USER_EMAIL, false, true, "ROLE_USER");
    }
}
