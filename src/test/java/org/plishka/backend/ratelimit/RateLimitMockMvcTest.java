package org.plishka.backend.ratelimit;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.plishka.backend.controller.BaseControllerTest;
import org.plishka.backend.dto.admin.review.AdminReviewDetailDto;
import org.plishka.backend.dto.admin.review.AdminReviewFeaturedRequestDto;
import org.plishka.backend.dto.auth.AuthResponseDto;
import org.plishka.backend.dto.auth.LoginRequestDto;
import org.plishka.backend.dto.cart.AddCartItemRequestDto;
import org.plishka.backend.dto.cart.CartSummaryDto;
import org.plishka.backend.dto.file.PresignDownloadRequestDto;
import org.plishka.backend.dto.file.PresignDownloadResponseDto;
import org.plishka.backend.service.VersionService;
import org.plishka.backend.service.admin.review.AdminReviewService;
import org.plishka.backend.service.auth.AuthService;
import org.plishka.backend.service.cart.CartService;
import org.plishka.backend.service.file.FilePresignService;
import org.plishka.backend.service.user.EmailChangeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "backend.rate-limit.enabled=true",
        "backend.rate-limit.policies[auth-login-email].bandwidths[0].capacity=5",
        "backend.rate-limit.policies[auth-login-email].bandwidths[0].period=1h",
        "backend.rate-limit.policies[cart-write].bandwidths[0].capacity=120",
        "backend.rate-limit.policies[cart-write].bandwidths[0].period=1h",
        "backend.rate-limit.policies[file-download-presign].bandwidths[0].capacity=300",
        "backend.rate-limit.policies[file-download-presign].bandwidths[0].period=1h",
        "backend.rate-limit.policies[public-read].bandwidths[0].capacity=600",
        "backend.rate-limit.policies[public-read].bandwidths[0].period=1h",
        "backend.rate-limit.policies[authenticated-read].bandwidths[0].capacity=600",
        "backend.rate-limit.policies[authenticated-read].bandwidths[0].period=1h",
        "backend.rate-limit.policies[admin-write].bandwidths[0].capacity=2",
        "backend.rate-limit.policies[admin-write].bandwidths[0].period=1h"
})
class RateLimitMockMvcTest extends BaseControllerTest {
    private static final long USER_ID = 41L;
    private static final long PRODUCT_ID = 10L;
    private static final String USER_EMAIL = "customer@example.com";
    private static final String DEVICE_ID = "11111111-1111-1111-1111-111111111111";
    private static final String PASSWORD = "Password1!";
    private static final String EMAIL_ACTION_TOKEN = "ABC1ABC1ABC1ABC1ABC1ABC1ABC1ABC1ABC1ABC1ABC";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private EmailChangeService emailChangeService;

    @MockitoBean
    private FilePresignService filePresignService;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private VersionService versionService;

    @MockitoBean
    private AdminReviewService adminReviewService;

    @BeforeEach
    void setUp() {
        when(authService.login(any(LoginRequestDto.class), anyString()))
                .thenReturn(new AuthResponseDto("access-token", "refresh-token"));
        when(filePresignService.presignDownload(any(PresignDownloadRequestDto.class)))
                .thenReturn(PresignDownloadResponseDto.builder()
                        .s3Key("products/main.jpg")
                        .downloadUrl("https://storage.example.com/products/main.jpg")
                        .method("GET")
                        .expiresAt(Instant.now())
                        .build());
        when(cartService.addItem(eq(USER_ID), any(AddCartItemRequestDto.class)))
                .thenReturn(new CartSummaryDto(List.of(), 0L));
        when(versionService.getCurrentVersion()).thenReturn(1);
        when(adminReviewService.updateFeatured(anyLong(), any(AdminReviewFeaturedRequestDto.class)))
                .thenReturn(new AdminReviewDetailDto(
                        1L,
                        "Reviewer",
                        "Review content",
                        Instant.now(),
                        true,
                        List.of()
                ));
    }

    @Test
    void login_ShouldReturn429AfterLimitExceeded() throws Exception {
        for (int i = 0; i < 5; i++) {
            performLogin("login@example.com", "10.1.0.1")
                    .andExpect(status().isOk());
        }

        performLogin("login@example.com", "10.1.0.1")
                .andExpect(rateLimitExceeded());
    }

    @Test
    void login_ShouldLimitSameEmailAcrossDifferentIps() throws Exception {
        for (int i = 0; i < 5; i++) {
            performLogin("distributed-login@example.com", "10.1.0.%d".formatted(i + 10))
                    .andExpect(status().isOk());
        }

        performLogin("distributed-login@example.com", "10.1.0.99")
                .andExpect(rateLimitExceeded());
    }

    @Test
    void login_ShouldKeepBodyReadableAfterRateLimitExtraction() throws Exception {
        performLogin("body-readable@example.com", "10.1.0.2")
                .andExpect(status().isOk());

        ArgumentCaptor<LoginRequestDto> requestCaptor = ArgumentCaptor.forClass(LoginRequestDto.class);
        verify(authService).login(requestCaptor.capture(), eq(DEVICE_ID));

        LoginRequestDto request = requestCaptor.getValue();
        assertEquals("body-readable@example.com", request.email());
        assertEquals(PASSWORD, request.password());
    }

    @Test
    void login_WithMalformedJson_ShouldReturnBadRequestInsteadOfRateLimitResponse() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Device-Id", DEVICE_ID)
                        .content("""
                                {
                                  "email": "malformed@example.com",
                                """)
                        .with(remoteAddr("10.1.0.3")))
                .andExpect(status().isBadRequest());

        verify(authService, never()).login(any(LoginRequestDto.class), anyString());
    }

    @Test
    void resendVerification_ShouldLimitByEmail() throws Exception {
        performEmailRequest("/auth/resend-verification", "resend@example.com", "10.1.1.1")
                .andExpect(status().isOk());

        performEmailRequest("/auth/resend-verification", "resend@example.com", "10.1.1.2")
                .andExpect(rateLimitExceeded());
    }

    @Test
    void forgotPassword_ShouldLimitByEmail() throws Exception {
        performEmailRequest("/auth/forgot-password", "forgot@example.com", "10.1.2.1")
                .andExpect(status().isOk());

        performEmailRequest("/auth/forgot-password", "forgot@example.com", "10.1.2.2")
                .andExpect(rateLimitExceeded());
    }

    @Test
    void register_ShouldLimitByEmailAndIp() throws Exception {
        for (int i = 0; i < 3; i++) {
            performRegister("shared-register@example.com", "10.1.3.%d".formatted(i))
                    .andExpect(status().isOk());
        }

        performRegister("shared-register@example.com", "10.1.3.99")
                .andExpect(rateLimitExceeded());

        for (int i = 0; i < 10; i++) {
            performRegister("ip-register-%d@example.com".formatted(i), "10.1.4.1")
                    .andExpect(status().isOk());
        }

        performRegister("ip-register-over-limit@example.com", "10.1.4.1")
                .andExpect(rateLimitExceeded());
    }

    @Test
    void resetPassword_ShouldLimitByTokenHash() throws Exception {
        for (int i = 0; i < 10; i++) {
            performResetPassword(EMAIL_ACTION_TOKEN, "10.1.5.%d".formatted(i))
                    .andExpect(status().isOk());
        }

        performResetPassword(EMAIL_ACTION_TOKEN, "10.1.5.99")
                .andExpect(rateLimitExceeded());
    }

    @Test
    void presignDownload_ShouldReturn429AfterLimitExceeded() throws Exception {
        for (int i = 0; i < 300; i++) {
            performPresignDownload("10.1.6.1")
                    .andExpect(status().isOk());
        }

        performPresignDownload("10.1.6.1")
                .andExpect(rateLimitExceeded());
    }

    @Test
    void cartWrite_ShouldLimitByUserId() throws Exception {
        for (int i = 0; i < 120; i++) {
            performAddCartItem(USER_ID, "10.1.7.1")
                    .andExpect(status().isOk());
        }

        performAddCartItem(USER_ID, "10.1.7.2")
                .andExpect(rateLimitExceeded());
    }

    @Test
    void publicRead_ShouldReturn429OnlyAfterHighCommonLimit() throws Exception {
        for (int i = 0; i < 600; i++) {
            mockMvc.perform(get("/version")
                            .with(remoteAddr("10.1.8.1")))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/version")
                        .with(remoteAddr("10.1.8.1")))
                .andExpect(rateLimitExceeded());
    }

    @Test
    void authenticatedRead_ShouldIncludeGetCartInCommonLimit() throws Exception {
        when(cartService.getCart(USER_ID)).thenReturn(new CartSummaryDto(List.of(), 0L));

        for (int i = 0; i < 600; i++) {
            mockMvc.perform(get("/cart")
                            .with(authenticatedUser(USER_ID, USER_EMAIL))
                            .with(remoteAddr("10.1.9.1")))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/cart")
                        .with(authenticatedUser(USER_ID, USER_EMAIL))
                        .with(remoteAddr("10.1.9.2")))
                .andExpect(rateLimitExceeded());
    }

    @Test
    void adminPatch_ShouldUseAdminWriteLimit() throws Exception {
        for (int i = 0; i < 2; i++) {
            performUpdateReviewFeatured()
                    .andExpect(status().isOk());
        }

        performUpdateReviewFeatured()
                .andExpect(rateLimitExceeded());
    }

    private ResultActions performLogin(String email, String ip) throws Exception {
        return mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Device-Id", DEVICE_ID)
                .content("""
                        {
                          "email": "%s",
                          "password": "%s"
                        }
                        """.formatted(email, PASSWORD))
                .with(remoteAddr(ip)));
    }

    private ResultActions performEmailRequest(String path, String email, String ip) throws Exception {
        return mockMvc.perform(post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email": "%s"
                        }
                        """.formatted(email))
                .with(remoteAddr(ip)));
    }

    private ResultActions performRegister(String email, String ip) throws Exception {
        return mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "Test User",
                          "email": "%s",
                          "password": "%s",
                          "confirmPassword": "%s"
                        }
                        """.formatted(email, PASSWORD, PASSWORD))
                .with(remoteAddr(ip)));
    }

    private ResultActions performResetPassword(String token, String ip) throws Exception {
        return mockMvc.perform(post("/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "token": "%s",
                          "password": "%s",
                          "confirmPassword": "%s"
                        }
                        """.formatted(token, PASSWORD, PASSWORD))
                .with(remoteAddr(ip)));
    }

    private ResultActions performPresignDownload(String ip) throws Exception {
        return mockMvc.perform(post("/files/presign/download")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new PresignDownloadRequestDto("products/main.jpg")))
                .with(remoteAddr(ip)));
    }

    private ResultActions performAddCartItem(Long userId, String ip) throws Exception {
        return mockMvc.perform(post("/cart/items")
                .with(authenticatedUser(userId, USER_EMAIL))
                .with(remoteAddr(ip))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AddCartItemRequestDto(PRODUCT_ID, 1))));
    }

    private ResultActions performUpdateReviewFeatured() throws Exception {
        return mockMvc.perform(patch("/admin/reviews/1/featured")
                .with(authenticatedAdmin())
                .with(remoteAddr("10.1.10.1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "featured": true
                        }
                        """));
    }

    private static RequestPostProcessor remoteAddr(String ip) {
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }

    private static org.springframework.test.web.servlet.ResultMatcher rateLimitExceeded() {
        return result -> {
            status().isTooManyRequests().match(result);
            header().exists(HttpHeaders.RETRY_AFTER).match(result);
            jsonPath("$.timestamp").exists().match(result);
            jsonPath("$.status").value(429).match(result);
            jsonPath("$.error").value("Too Many Requests").match(result);
            jsonPath("$.message").value("Too many requests. Please try again later.").match(result);
            jsonPath("$.path").exists().match(result);
        };
    }
}
