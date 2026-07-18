package org.plishka.backend.controller.admin;

import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.plishka.backend.controller.BaseControllerTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminEndpointSmokeTest extends BaseControllerTest {
    private static final long MISSING_ID = 999_999L;

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest(name = "{0}")
    @MethodSource("adminEndpointRequests")
    void adminEndpoint_ShouldReturn401_WhenAnonymous(
            String name,
            Supplier<MockHttpServletRequestBuilder> requestSupplier,
            Set<HttpStatus> expectedActiveAdminStatuses
    ) throws Exception {
        int status = mockMvc.perform(requestSupplier.get())
                .andReturn()
                .getResponse()
                .getStatus();

        assertEquals(HttpStatus.UNAUTHORIZED.value(), status, name + " must reject anonymous access");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("adminEndpointRequests")
    void adminEndpoint_ShouldReturn403_WhenAuthenticatedUserIsNotAdmin(
            String name,
            Supplier<MockHttpServletRequestBuilder> requestSupplier,
            Set<HttpStatus> expectedActiveAdminStatuses
    ) throws Exception {
        int status = mockMvc.perform(requestSupplier.get().with(user("user@example.com").roles("USER")))
                .andReturn()
                .getResponse()
                .getStatus();

        assertEquals(HttpStatus.FORBIDDEN.value(), status, name + " must require admin role");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("adminEndpointRequests")
    void adminEndpoint_ShouldNotRejectActiveAdminOrReturnServerError(
            String name,
            Supplier<MockHttpServletRequestBuilder> requestSupplier,
            Set<HttpStatus> expectedActiveAdminStatuses
    ) throws Exception {
        MvcResult result = mockMvc.perform(requestSupplier.get().with(authenticatedAdmin()).accept(MediaType.APPLICATION_JSON))
                .andReturn();
        int status = result.getResponse().getStatus();

        assertTrue(
                expectedActiveAdminStatuses.stream().map(HttpStatus::value).anyMatch(expected -> expected == status),
                () -> "%s returned %d, expected one of %s".formatted(name, status, expectedActiveAdminStatuses)
        );
        assertJsonContract(result, HttpStatus.valueOf(status));
    }

    private static Stream<Arguments> adminEndpointRequests() {
        return Stream.of(
                request("GET /admin/about", () -> get("/admin/about"), HttpStatus.OK),
                request("PUT /admin/about", () -> putJson("/admin/about", "{}"), HttpStatus.BAD_REQUEST),
                request(
                        "POST /admin/about/media/attach",
                        () -> postJson("/admin/about/media/attach", "{}"),
                        HttpStatus.BAD_REQUEST
                ),
                request(
                        "PUT /admin/about/media/order",
                        () -> putJson("/admin/about/media/order", "{}"),
                        HttpStatus.BAD_REQUEST
                ),
                request("DELETE /admin/about/media", () -> delete("/admin/about/media"), HttpStatus.NO_CONTENT),
                request("DELETE /admin/about/media/{mediaId}",
                        () -> delete("/admin/about/media/{mediaId}", MISSING_ID), HttpStatus.NOT_FOUND),

                request("GET /admin/categories", () -> get("/admin/categories"), HttpStatus.OK),
                request("POST /admin/categories", () -> postJson("/admin/categories", "{}"), HttpStatus.BAD_REQUEST),
                request(
                        "PUT /admin/categories/{id}",
                        () -> putJson("/admin/categories/{id}", MISSING_ID, "{}"),
                        HttpStatus.BAD_REQUEST
                ),
                request("DELETE /admin/categories/{id}",
                        () -> delete("/admin/categories/{id}", MISSING_ID).param("strategy", "KEEP_PRODUCTS"),
                        HttpStatus.NOT_FOUND),

                request("GET /admin/contacts-page", () -> get("/admin/contacts-page"), HttpStatus.OK),
                request(
                        "PUT /admin/contacts-page",
                        () -> putJson("/admin/contacts-page", "{}"),
                        HttpStatus.OK
                ),
                request("POST /admin/contacts-page/social-links",
                        () -> postJson("/admin/contacts-page/social-links", "{}"), HttpStatus.BAD_REQUEST),
                request("PUT /admin/contacts-page/social-links/{socialLinkId}",
                        () -> putJson("/admin/contacts-page/social-links/{socialLinkId}", MISSING_ID, "{}"),
                        HttpStatus.BAD_REQUEST),
                request("DELETE /admin/contacts-page/social-links/{socialLinkId}",
                        () -> delete("/admin/contacts-page/social-links/{socialLinkId}", MISSING_ID),
                        HttpStatus.NOT_FOUND),

                request(
                        "POST /admin/files/presign/upload",
                        () -> postJson("/admin/files/presign/upload", "{}"),
                        HttpStatus.BAD_REQUEST
                ),

                request("GET /admin/home-page", () -> get("/admin/home-page"), HttpStatus.OK),
                request("PUT /admin/home-page", () -> putJson("/admin/home-page", "{}"), HttpStatus.BAD_REQUEST),

                request("GET /admin/products", () -> get("/admin/products"), HttpStatus.OK),
                request("GET /admin/products/{id}", () -> get("/admin/products/{id}", MISSING_ID), HttpStatus.NOT_FOUND),
                request("POST /admin/products", () -> postJson("/admin/products", "{}"), HttpStatus.BAD_REQUEST),
                request(
                        "PUT /admin/products/{id}",
                        () -> putJson("/admin/products/{id}", MISSING_ID, "{}"),
                        HttpStatus.BAD_REQUEST
                ),
                request("DELETE /admin/products/{id}", () -> delete("/admin/products/{id}", MISSING_ID),
                        HttpStatus.NOT_FOUND),
                request("POST /admin/products/bulk/delete", () -> postJson("/admin/products/bulk/delete", "{}"),
                        HttpStatus.BAD_REQUEST),
                request("POST /admin/products/bulk/price", () -> postJson("/admin/products/bulk/price", "{}"),
                        HttpStatus.BAD_REQUEST),
                request("POST /admin/products/bulk/category", () -> postJson("/admin/products/bulk/category", "{}"),
                        HttpStatus.BAD_REQUEST),
                request("PUT /admin/products/home", () -> putJson("/admin/products/home", "{}"),
                        HttpStatus.BAD_REQUEST),
                request("PUT /admin/products/home-order", () -> putJson("/admin/products/home-order", "{}"),
                        HttpStatus.BAD_REQUEST),
                request("POST /admin/products/{id}/media/attach",
                        () -> postJson("/admin/products/{id}/media/attach", MISSING_ID, "{}"),
                        HttpStatus.BAD_REQUEST),
                request("DELETE /admin/products/{productId}/media/{mediaId}",
                        () -> delete("/admin/products/{productId}/media/{mediaId}", MISSING_ID, MISSING_ID),
                        HttpStatus.NOT_FOUND),
                request("DELETE /admin/products/{productId}/media",
                        () -> delete("/admin/products/{productId}/media", MISSING_ID), HttpStatus.NOT_FOUND),
                request("PUT /admin/products/{productId}/media/{mediaId}/primary",
                        () -> put("/admin/products/{productId}/media/{mediaId}/primary", MISSING_ID, MISSING_ID),
                        HttpStatus.NOT_FOUND),

                request("GET /admin/reviews", () -> get("/admin/reviews"), HttpStatus.OK),
                request("GET /admin/reviews/{id}", () -> get("/admin/reviews/{id}", MISSING_ID), HttpStatus.NOT_FOUND),
                request("POST /admin/reviews", () -> postJson("/admin/reviews", "{}"), HttpStatus.BAD_REQUEST),
                request("PUT /admin/reviews/{id}", () -> putJson("/admin/reviews/{id}", MISSING_ID, "{}"),
                        HttpStatus.BAD_REQUEST),
                request("DELETE /admin/reviews/{id}", () -> delete("/admin/reviews/{id}", MISSING_ID),
                        HttpStatus.NOT_FOUND),
                request("PATCH /admin/reviews/{id}/featured",
                        () -> patchJson("/admin/reviews/{id}/featured", MISSING_ID, "{}"), HttpStatus.BAD_REQUEST),
                request("POST /admin/reviews/{id}/media/attach",
                        () -> postJson("/admin/reviews/{id}/media/attach", MISSING_ID, "{}"),
                        HttpStatus.BAD_REQUEST),
                request("DELETE /admin/reviews/{reviewId}/media/{mediaId}",
                        () -> delete("/admin/reviews/{reviewId}/media/{mediaId}", MISSING_ID, MISSING_ID),
                        HttpStatus.NOT_FOUND),
                request("DELETE /admin/reviews/{reviewId}/media",
                        () -> delete("/admin/reviews/{reviewId}/media", MISSING_ID), HttpStatus.NOT_FOUND),
                request("PUT /admin/reviews/{reviewId}/media/{mediaId}/primary",
                        () -> put("/admin/reviews/{reviewId}/media/{mediaId}/primary", MISSING_ID, MISSING_ID),
                        HttpStatus.NOT_FOUND),

                request("GET /admin/settings", () -> get("/admin/settings"), HttpStatus.OK),
                request("PUT /admin/settings", () -> putJson("/admin/settings", "{}"), HttpStatus.BAD_REQUEST),

                request("GET /admin/users", () -> get("/admin/users"), HttpStatus.OK),
                request("GET /admin/users/banned", () -> get("/admin/users/banned"), HttpStatus.OK),
                request("PUT /admin/users/{id}/ban", () -> put("/admin/users/{id}/ban", MISSING_ID),
                        HttpStatus.NOT_FOUND),
                request("PUT /admin/users/{id}/unban", () -> put("/admin/users/{id}/unban", MISSING_ID),
                        HttpStatus.NOT_FOUND),
                request("POST /admin/users/ban/bulk", () -> postJson("/admin/users/ban/bulk", "{}"),
                        HttpStatus.BAD_REQUEST),
                request("POST /admin/users/unban/bulk", () -> postJson("/admin/users/unban/bulk", "{}"),
                        HttpStatus.BAD_REQUEST)
        );
    }

    private void assertJsonContract(MvcResult result, HttpStatus status) throws Exception {
        if (result.getResponse().getContentAsString().isBlank()) {
            return;
        }

        content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON).match(result);
        if (status.isError()) {
            jsonPath("$.status").value(status.value()).match(result);
            jsonPath("$.error").exists().match(result);
            jsonPath("$.message").exists().match(result);
            jsonPath("$.path").exists().match(result);
        }
    }

    private static Arguments request(
            String name,
            Supplier<MockHttpServletRequestBuilder> requestSupplier,
            HttpStatus expectedActiveAdminStatus
    ) {
        return Arguments.of(name, requestSupplier, Set.of(expectedActiveAdminStatus));
    }

    private static MockHttpServletRequestBuilder postJson(String path, String content) {
        return post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(content);
    }

    private static MockHttpServletRequestBuilder postJson(String path, Object uriVar, String content) {
        return post(path, uriVar)
                .contentType(MediaType.APPLICATION_JSON)
                .content(content);
    }

    private static MockHttpServletRequestBuilder putJson(String path, String content) {
        return put(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(content);
    }

    private static MockHttpServletRequestBuilder putJson(String path, Object uriVar, String content) {
        return put(path, uriVar)
                .contentType(MediaType.APPLICATION_JSON)
                .content(content);
    }

    private static MockHttpServletRequestBuilder patchJson(String path, Object uriVar, String content) {
        return patch(path, uriVar)
                .contentType(MediaType.APPLICATION_JSON)
                .content(content);
    }
}
