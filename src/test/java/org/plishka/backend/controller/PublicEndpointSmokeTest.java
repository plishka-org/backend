package org.plishka.backend.controller;

import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.plishka.backend.service.notification.email.transport.resend.ResendEmailTransport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicEndpointSmokeTest extends BaseControllerTest {
    private static final String INVALID_EMAIL_ACTION_TOKEN = "invalid-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ResendEmailTransport resendEmailTransport;

    @ParameterizedTest(name = "{0}")
    @MethodSource("publicEndpointRequests")
    void publicEndpoint_ShouldReturnExpectedContractStatus(
            String name,
            Supplier<MockHttpServletRequestBuilder> requestSupplier,
            HttpStatus expectedStatus
    ) throws Exception {
        MvcResult result = mockMvc.perform(requestSupplier.get().accept(MediaType.APPLICATION_JSON))
                .andReturn();

        assertEquals(expectedStatus.value(), result.getResponse().getStatus(), name);
        assertJsonContract(name, result, expectedStatus);
    }

    private static Stream<Arguments> publicEndpointRequests() {
        return Stream.of(
                request("GET /health", () -> get("/health"), HttpStatus.OK),
                request("GET /version", () -> get("/version"), HttpStatus.OK),
                request("GET /home", () -> get("/home"), HttpStatus.OK),
                request("GET /about", () -> get("/about"), HttpStatus.OK),
                request("GET /contacts-page", () -> get("/contacts-page"), HttpStatus.OK),
                request("GET /settings", () -> get("/settings"), HttpStatus.OK),
                request("GET /categories", () -> get("/categories"), HttpStatus.OK),
                request("GET /products", () -> get("/products"), HttpStatus.OK),
                request("GET /products/{id}", () -> get("/products/{id}", 999_999L), HttpStatus.NOT_FOUND),
                request(
                        "GET /products/{id}/related",
                        () -> get("/products/{id}/related", 999_999L),
                        HttpStatus.NOT_FOUND
                ),
                request("GET /reviews", () -> get("/reviews"), HttpStatus.OK),
                request("GET /reviews/{id}", () -> get("/reviews/{id}", 999_999L), HttpStatus.NOT_FOUND),
                request(
                        "GET /auth/verify",
                        () -> get("/auth/verify").param("token", INVALID_EMAIL_ACTION_TOKEN),
                        HttpStatus.BAD_REQUEST
                ),
                request("POST /auth/register", () -> postJson("/auth/register", "{}"), HttpStatus.BAD_REQUEST),
                request(
                        "POST /auth/resend-verification",
                        () -> postJson("/auth/resend-verification", "{}"),
                        HttpStatus.BAD_REQUEST
                ),
                request(
                        "POST /auth/forgot-password",
                        () -> postJson("/auth/forgot-password", "{}"),
                        HttpStatus.BAD_REQUEST
                ),
                request("POST /auth/reset-password", () -> postJson("/auth/reset-password", "{}"), HttpStatus.BAD_REQUEST),
                request(
                        "POST /auth/verify-email-change",
                        () -> postJson("/auth/verify-email-change", "{}"),
                        HttpStatus.BAD_REQUEST
                ),
                request("POST /auth/login", () -> postJson("/auth/login", "{}"), HttpStatus.BAD_REQUEST),
                request("POST /auth/refresh", () -> postJson("/auth/refresh", "{}"), HttpStatus.BAD_REQUEST),
                request(
                        "POST /files/presign/download",
                        () -> postJson("/files/presign/download", "{}"),
                        HttpStatus.BAD_REQUEST
                )
        );
    }

    private void assertJsonContract(String name, MvcResult result, HttpStatus expectedStatus) throws Exception {
        if (result.getResponse().getContentAsString().isBlank()) {
            return;
        }

        content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON).match(result);
        if (expectedStatus.isError()) {
            jsonPath("$.status").value(expectedStatus.value()).match(result);
            jsonPath("$.error").exists().match(result);
            jsonPath("$.message").exists().match(result);
            jsonPath("$.path").exists().match(result);
            return;
        }

        assertEquals(HttpStatus.OK, expectedStatus, name + " unexpected non-error response status");
    }

    private static Arguments request(
            String name,
            Supplier<MockHttpServletRequestBuilder> requestSupplier,
            HttpStatus expectedStatus
    ) {
        return Arguments.of(name, requestSupplier, expectedStatus);
    }

    private static MockHttpServletRequestBuilder postJson(String path, String content) {
        return post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(content);
    }
}
