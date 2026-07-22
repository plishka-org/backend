package org.plishka.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.plishka.backend.openapi.operation.OperationDocumentationRegistry;
import org.plishka.backend.openapi.support.OpenApiExampleValues;
import org.plishka.backend.service.notification.email.transport.resend.ResendEmailTransport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "springdoc.api-docs.enabled=true",
        "springdoc.swagger-ui.enabled=true"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiDocumentationSmokeTest {
    private static final Set<String> HTTP_METHODS = Set.of(
            "get",
            "put",
            "post",
            "delete",
            "patch",
            "options",
            "head",
            "trace"
    );

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private OperationDocumentationRegistry operationDocumentationRegistry;

    @MockitoBean
    private ResendEmailTransport resendEmailTransport;

    @Test
    void apiDocsAreGeneratedWithSecurityPathsAndSchemas() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Plishka Backend API"))
                .andExpect(jsonPath("$.servers[0].url").value("/api"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.paths['/error']").doesNotExist())
                .andExpect(jsonPath("$.paths['/auth/login'].post.operationId").value("login"))
                .andExpect(jsonPath("$.paths['/auth/login'].post.security").doesNotExist())
                .andExpect(jsonPath("$.paths['/auth/login'].post.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/auth/login'].post.responses['403']").exists())
                .andExpect(jsonPath("$.paths['/auth/refresh'].post.security").doesNotExist())
                .andExpect(jsonPath("$.paths['/auth/refresh'].post.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/auth/refresh'].post.responses['403']").exists())
                .andExpect(jsonPath("$.paths['/auth/verify'].get.responses['302']").exists())
                .andExpect(jsonPath("$.paths['/auth/verify'].get.responses['302'].headers.Location").exists())
                .andExpect(jsonPath("$.paths['/auth/verify'].get.parameters[0].schema.minLength").value(43))
                .andExpect(jsonPath("$.paths['/auth/verify'].get.parameters[0].schema.maxLength").value(43))
                .andExpect(jsonPath("$.paths['/auth/verify'].get.parameters[0].schema.pattern")
                        .value("^[A-Za-z0-9_-]{43}$"))
                .andExpect(jsonPath("$.paths['/auth/verify'].get.parameters[0].example")
                        .value(OpenApiExampleValues.EMAIL_ACTION_TOKEN))
                .andExpect(jsonPath("$.paths['/orders'].post.operationId").value("createOrder"))
                .andExpect(jsonPath("$.paths['/admin/products'].post.operationId").value("adminCreateProduct"))
                .andExpect(jsonPath("$.paths['/admin/products'].post.security[0].bearerAuth").exists())
                .andExpect(jsonPath("$.paths['/admin/products'].post.security[1]").doesNotExist())
                .andExpect(jsonPath("$.paths['/admin/products'].post.tags[0]").value("Admin - Products"))
                .andExpect(jsonPath("$.paths['/admin/products/{id}/media/attach'].post.tags[0]")
                        .value("Admin - Product Media"))
                .andExpect(jsonPath("$.paths['/admin/users'].get.tags[0]").value("Admin - Users"))
                .andExpect(jsonPath("$.paths['/admin/settings'].get.tags[0]").value("Admin - Settings"))
                .andExpect(jsonPath("$.paths['/admin/orders'].get.operationId").value("adminGetOrders"))
                .andExpect(jsonPath("$.paths['/admin/orders/{id}'].get.operationId").value("adminGetOrder"))
                .andExpect(jsonPath("$.paths['/admin/callback-requests'].get.operationId")
                        .value("adminGetCallbackRequests"))
                .andExpect(jsonPath("$.paths['/admin/categories/order'].put.operationId")
                        .value("adminUpdateCategoryOrder"))
                .andExpect(jsonPath("$.components.schemas.LoginRequestDto").exists())
                .andExpect(jsonPath("$.components.schemas.CreateOrderRequestDto").exists())
                .andExpect(jsonPath("$.components.schemas.ErrorResponseDto").exists())
                .andExpect(jsonPath("$.components.schemas.ValidationErrorResponseDto").exists())
                .andExpect(jsonPath("$.components.responses.BadRequest").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.VerifyEmailChangeRequestDto.properties.token.example")
                        .value(OpenApiExampleValues.EMAIL_ACTION_TOKEN))
                .andExpect(jsonPath("$.components.schemas.ResetPasswordRequestDto.properties.token.example")
                        .value(OpenApiExampleValues.EMAIL_ACTION_TOKEN))
                .andExpect(jsonPath("$.paths['/about'].get.responses['400']").doesNotExist())
                .andExpect(jsonPath(
                        "$.paths['/auth/login'].post.responses['400'].content['application/json']"
                                + ".examples.validationError.value.path"
                ).value("/api/auth/login"))
                .andExpect(jsonPath(
                        "$.paths['/auth/login'].post.responses['400'].content['application/json']"
                                + ".examples.validationError.value.fieldErrors[0]"
                ).value("email: Email must be a valid email address"))
                .andExpect(jsonPath(
                        "$.paths['/auth/login'].post.responses['400'].content['application/json']"
                                + ".examples.validationError.value.fieldErrors[1]"
                ).value("password: Password must be between 8 and 64 characters long"))
                .andExpect(jsonPath(
                        "$.paths['/products/{id}'].get.responses['400'].content['application/json']"
                                + ".examples.validationError.value.path"
                ).value("/api/products/123"))
                .andExpect(jsonPath(
                        "$.paths['/products/{id}'].get.responses['400'].content['application/json']"
                                + ".examples.validationError.value.fieldErrors[0]"
                ).value("id: must be greater than 0"))
                .andExpect(jsonPath(
                        "$.paths['/orders'].post.responses['400'].content['application/json']"
                                + ".schema.oneOf[0].$ref"
                ).value("#/components/schemas/ErrorResponseDto"))
                .andExpect(jsonPath(
                        "$.paths['/orders'].post.responses['400'].content['application/json']"
                                + ".schema.oneOf[1].$ref"
                ).value("#/components/schemas/ValidationErrorResponseDto"))
                .andExpect(jsonPath(
                        "$.paths['/admin/products'].get.responses['401'].content['application/json'].examples"
                                + ".invalidOrMissingCredentials.value.status"
                ).value(401))
                .andExpect(jsonPath(
                        "$.paths['/admin/products'].get.responses['401'].content['application/json'].examples"
                                + ".invalidOrMissingCredentials.value.path"
                ).value("/api/admin/products"))
                .andExpect(jsonPath("$.paths['/admin/products'].get.responses['401'].$ref").doesNotExist())
                .andExpect(jsonPath(
                        "$.paths['/auth/login'].post.responses['401'].content['application/json']"
                                + ".examples.invalidCredentials.value.message"
                ).value("Invalid email or password"))
                .andExpect(jsonPath(
                        "$.paths['/auth/login'].post.responses['401'].content['application/json']"
                                + ".examples.invalidRefreshToken"
                ).doesNotExist())
                .andExpect(jsonPath(
                        "$.paths['/auth/refresh'].post.responses['401'].content['application/json']"
                                + ".examples.refreshTokenNotFound.value.message"
                ).value("Refresh token not found"))
                .andExpect(jsonPath(
                        "$.paths['/auth/refresh'].post.responses['401'].content['application/json']"
                                + ".examples.invalidCredentials"
                ).doesNotExist())
                .andExpect(jsonPath(
                        "$.paths['/admin/products'].get.responses['403'].content['application/json'].examples.accessDenied"
                                + ".value.message"
                ).value("You do not have permission to access this resource"))
                .andExpect(jsonPath(
                        "$.paths['/admin/products'].get.responses['403'].content['application/json'].examples.shopModeDisabled"
                ).doesNotExist())
                .andExpect(jsonPath("$.paths['/admin/products'].get.responses['403'].$ref").doesNotExist())
                .andExpect(jsonPath(
                        "$.paths['/cart'].get.responses['403'].content['application/json'].examples"
                                + ".shopModeDisabled.value.message"
                ).value("Shop mode is disabled"))
                .andExpect(jsonPath(
                        "$.paths['/cart'].get.responses['403'].content['application/json'].examples"
                                + ".shopModeDisabled.value.path"
                ).value("/api/cart"))
                .andExpect(jsonPath(
                        "$.paths['/cart'].get.responses['403'].content['application/json'].examples"
                                + ".accessDenied.value.message"
                ).value("You do not have permission to access this resource"))
                .andExpect(jsonPath(
                        "$.paths['/cart'].delete.responses['403'].content['application/json'].examples"
                                + ".shopModeDisabled.value.message"
                ).value("Shop mode is disabled"))
                .andExpect(jsonPath(
                        "$.paths['/cart/items'].post.responses['403'].content['application/json'].examples"
                                + ".shopModeDisabled.value.message"
                ).value("Shop mode is disabled"))
                .andExpect(jsonPath("$.paths['/cart/items'].post.responses['404']").exists())
                .andExpect(jsonPath(
                        "$.paths['/cart/items/{productId}'].put.responses['403'].content['application/json']"
                                + ".examples.shopModeDisabled.value.path"
                ).value("/api/cart/items/123"))
                .andExpect(jsonPath(
                        "$.paths['/cart/items/{productId}'].delete.responses['403'].content['application/json']"
                                + ".examples.shopModeDisabled.value.message"
                ).value("Shop mode is disabled"))
                .andExpect(jsonPath(
                        "$.paths['/cart/merge'].post.responses['403'].content['application/json'].examples"
                                + ".shopModeDisabled.value.message"
                ).value("Shop mode is disabled"))
                .andExpect(jsonPath(
                        "$.paths['/orders'].post.responses['403'].content['application/json'].examples"
                                + ".shopModeDisabled.value.path"
                ).value("/api/orders"))
                .andExpect(jsonPath(
                        "$.paths['/users/me/orders/{orderId}/repeat'].post.responses['403']"
                                + ".content['application/json'].examples.shopModeDisabled.value.path"
                ).value("/api/users/me/orders/123/repeat"))
                .andExpect(jsonPath(
                        "$.paths['/cart/items/{productId}'].put.responses['403'].$ref"
                ).doesNotExist())
                .andExpect(jsonPath(
                        "$.paths['/cart'].delete.responses['404'].content['application/json'].examples"
                                + ".resourceNotFound.value.path"
                ).value("/api/cart"))
                .andExpect(jsonPath(
                        "$.paths['/cart'].delete.responses['404'].content['application/json'].examples"
                                + ".resourceNotFound.value.message"
                ).value("Cart not found for user id=123"))
                .andExpect(jsonPath("$.paths['/admin/files/presign/upload'].post.responses['404']").exists())
                .andExpect(jsonPath("$.paths['/admin/files/presign/upload'].post.responses['503']").exists())
                .andExpect(jsonPath("$.paths['/admin/files/presign/upload'].post.responses['503'].$ref")
                        .doesNotExist())
                .andExpect(jsonPath(
                        "$.paths['/admin/files/presign/upload'].post.requestBody.content['application/json']"
                                + ".examples.presignUploadRequest.value.checksumSha256Base64"
                ).value(OpenApiExampleValues.CHECKSUM_SHA256_BASE64))
                .andExpect(jsonPath(
                        "$.paths['/admin/files/presign/upload'].post.responses['200'].content['application/json']"
                                + ".examples.presignUpload.value.requiredHeaders['Content-Type']"
                ).value(OpenApiExampleValues.IMAGE_WEBP_CONTENT_TYPE))
                .andExpect(jsonPath(
                        "$.paths['/admin/files/presign/upload'].post.responses['200'].content['application/json']"
                                + ".examples.presignUpload.value.requiredHeaders['x-amz-checksum-sha256']"
                ).value(OpenApiExampleValues.CHECKSUM_SHA256_BASE64))
                .andExpect(jsonPath(
                        "$.paths['/admin/files/presign/upload'].post.responses['200'].content['application/json']"
                                + ".examples.presignUpload.value.requiredHeaders['x-amz-tagging']"
                ).value(OpenApiExampleValues.PENDING_UPLOAD_TAGGING))
                .andExpect(jsonPath(
                        "$.paths['/admin/files/presign/upload'].post.responses['503'].content['application/json']"
                                + ".examples.storageUnavailable.value.message"
                ).value("File storage is temporarily unavailable"))
                .andExpect(jsonPath(
                        "$.paths['/admin/files/presign/upload'].post.responses['503'].content['application/json']"
                                + ".examples.storageUnavailable.value.path"
                ).value("/api/admin/files/presign/upload"))
                .andExpect(jsonPath("$.paths['/admin/about/media/attach'].post.responses['503']").exists())
                .andExpect(jsonPath("$.paths['/admin/products/{id}/media/attach'].post.responses['503']").exists())
                .andExpect(jsonPath("$.paths['/admin/reviews/{id}/media/attach'].post.responses['503']").exists())
                .andExpect(jsonPath(
                        "$.paths['/admin/about/media/attach'].post.responses['503'].content['application/json']"
                                + ".examples.storageUnavailable.value.path"
                ).value("/api/admin/about/media/attach"))
                .andExpect(jsonPath(
                        "$.paths['/admin/products/{id}/media/attach'].post.responses['503'].content['application/json']"
                                + ".examples.storageUnavailable.value.path"
                ).value("/api/admin/products/123/media/attach"))
                .andExpect(jsonPath(
                        "$.paths['/admin/reviews/{id}/media/attach'].post.responses['503'].content['application/json']"
                                + ".examples.storageUnavailable.value.path"
                ).value("/api/admin/reviews/123/media/attach"))
                .andExpect(jsonPath("$.paths['/callback'].post.responses['409']").doesNotExist())
                .andExpect(jsonPath("$.paths['/admin/products/home'].put.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/admin/products/home'].put.responses['200'].content").doesNotExist())
                .andExpect(jsonPath("$.paths['/admin/products/home-order'].put.responses['200']").exists())
                .andExpect(jsonPath(
                        "$.paths['/admin/products/home-order'].put.responses['200'].content"
                ).doesNotExist())
                .andExpect(jsonPath("$.components.schemas.ProductSummaryDto.properties.price.type[1]").value("null"))
                .andExpect(jsonPath("$.components.schemas.ProductDetailDto.properties.price.type[1]").value("null"))
                .andExpect(jsonPath("$.components.schemas.HomePageProductDto.properties.price.type[1]").value("null"))
                .andExpect(jsonPath("$.components.schemas.ProductSummaryDto.properties.category.type[1]").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.ProductSummaryDto.properties.category.nullable").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.ProductDetailDto.properties.category.type[1]").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.ProductDetailDto.properties.category.nullable").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.HomePageProductDto.properties.category.type[1]").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.HomePageProductDto.properties.category.nullable").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.AdminProductDetailDto.properties.category.type")
                        .value("null"))
                .andExpect(jsonPath(
                        "$.paths['/products'].get.responses['200'].content['application/json'].examples"
                                + ".productsPage.value.content[0].category.categoryId"
                ).value(1))
                .andExpect(jsonPath(
                        "$.paths['/products'].get.responses['200'].content['application/json'].examples"
                                + ".productsPage.value.content[0].category.name"
                ).value(OpenApiExampleValues.ENDPOINT_CATEGORY_NAME))
                .andExpect(jsonPath(
                        "$.paths['/products/{id}'].get.responses['200'].content['application/json'].examples"
                                + ".productDetails.value.category.categoryId"
                ).value(1))
                .andExpect(jsonPath(
                        "$.paths['/products/{id}'].get.responses['200'].content['application/json'].examples"
                                + ".productDetails.value.category.name"
                ).value(OpenApiExampleValues.ENDPOINT_CATEGORY_NAME))
                .andExpect(jsonPath(
                        "$.paths['/products/{id}/related'].get.responses['200'].content['application/json'].examples"
                                + ".relatedProductsPage.value.content[0].category.categoryId"
                ).value(1))
                .andExpect(jsonPath(
                        "$.paths['/home'].get.responses['200'].content['application/json'].examples"
                                + ".homePage.value.products[0].category.name"
                ).value(OpenApiExampleValues.ENDPOINT_CATEGORY_NAME))
                .andExpect(jsonPath("$.paths['/admin/about'].put.responses['404']").doesNotExist())
                .andExpect(jsonPath("$.paths['/admin/contacts-page/social-links'].post.responses['404']")
                        .doesNotExist())
                .andExpect(jsonPath("$.paths['/admin/home-page'].put.responses['404']").doesNotExist())
                .andExpect(jsonPath("$.paths['/admin/settings'].get.responses['404']").doesNotExist())
                .andExpect(jsonPath("$.paths['/admin/settings'].put.responses['404']").doesNotExist())
                .andExpect(jsonPath(
                        "$.paths['/auth/verify-email-change'].post.responses['409'].content['application/json']"
                                + ".examples.duplicateResource.value.path"
                ).value("/api/auth/verify-email-change"))
                .andExpect(jsonPath(
                        "$.paths['/auth/verify-email-change'].post.responses['409'].content['application/json']"
                                + ".examples.duplicateResource.value.message"
                ).value("Email already exists"))
                .andExpect(jsonPath(
                        "$.paths['/auth/verify-email-change'].post.responses['409'].content['application/json']"
                                + ".examples.idempotencyConflict"
                ).doesNotExist())
                .andExpect(jsonPath("$.paths['/auth/verify-email-change'].post.responses['409'].$ref")
                        .doesNotExist())
                .andExpect(jsonPath(
                        "$.paths['/auth/verify-email-change'].post.responses['429'].content['application/json']"
                                + ".examples.rateLimitExceeded.value.path"
                ).value("/api/auth/verify-email-change"))
                .andExpect(jsonPath("$.paths['/auth/verify-email-change'].post.responses['429'].$ref")
                        .doesNotExist())
                .andExpect(jsonPath("$.paths['/auth/verify-email-change'].post.responses['429'].headers['Retry-After']")
                        .exists())
                .andExpect(jsonPath(
                        "$.paths['/auth/verify-email-change'].post.responses['500'].content['application/json']"
                                + ".examples.unexpectedError.value.path"
                ).value("/api/auth/verify-email-change"))
                .andExpect(jsonPath("$.paths['/auth/verify-email-change'].post.responses['500'].$ref")
                        .doesNotExist())
                .andReturn();

        String apiDocs = result.getResponse().getContentAsString();
        assertDocumentedOperationIdsExist(apiDocs);

        assertThat(apiDocs)
                .doesNotContain("Clothes created for everyday comfort.")
                .doesNotContain("We create comfortable clothes.")
                .doesNotContain("\"dress\"")
                .doesNotContain("Linen Dress")
                .doesNotContain("Dresses")
                .doesNotContain("refresh_token_example_without_real_value")
                .doesNotContain("password_reset_token_example_without_real_value")
                .doesNotContain("email_change_token_example_without_real_value")
                .doesNotContain("email_action_token_example_without_real_value")
                .doesNotContain("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb")
                .doesNotContain("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
                .doesNotContain("additionalProp1")
                .doesNotContain("additionalProp2")
                .doesNotContain("additionalProp3")
                .contains(OpenApiExampleValues.EMAIL_ACTION_TOKEN)
                .contains(OpenApiExampleValues.CHECKSUM_SHA256_BASE64)
                .contains("x-amz-tagging")
                .contains(OpenApiExampleValues.PENDING_UPLOAD_TAGGING)
                .doesNotContain("#/components/responses/Unauthorized")
                .doesNotContain("#/components/responses/Forbidden")
                .doesNotContain("#/components/responses/NotFound")
                .doesNotContain("#/components/responses/Conflict")
                .doesNotContain("#/components/responses/TooManyRequests")
                .doesNotContain("#/components/responses/InternalServerError")
                .doesNotContain("#/components/responses/StorageUnavailable");
    }

    private void assertDocumentedOperationIdsExist(String apiDocs) throws Exception {
        Set<String> generatedOperationIds = generatedOperationIds(apiDocs);

        assertThat(operationDocumentationRegistry.registeredOperationIds())
                .as("Every operationId customized by OperationDocumentationRegistry must exist in /v3/api-docs")
                .isSubsetOf(generatedOperationIds);
    }

    private Set<String> generatedOperationIds(String apiDocs) throws Exception {
        JsonNode paths = objectMapper.readTree(apiDocs).path("paths");
        Set<String> operationIds = new LinkedHashSet<>();

        Iterator<Map.Entry<String, JsonNode>> pathIterator = paths.fields();
        while (pathIterator.hasNext()) {
            Map.Entry<String, JsonNode> path = pathIterator.next();
            collectOperationIds(path.getKey(), path.getValue(), operationIds);
        }

        return operationIds;
    }

    private void collectOperationIds(String path, JsonNode pathItem, Set<String> operationIds) {
        Iterator<Map.Entry<String, JsonNode>> operationIterator = pathItem.fields();
        while (operationIterator.hasNext()) {
            Map.Entry<String, JsonNode> operation = operationIterator.next();
            if (!HTTP_METHODS.contains(operation.getKey())) {
                continue;
            }

            JsonNode operationId = operation.getValue().get("operationId");
            assertThat(operationId)
                    .as("Operation %s %s must have operationId", operation.getKey().toUpperCase(), path)
                    .isNotNull();

            String value = operationId.asText();
            assertThat(operationIds.add(value))
                    .as("Duplicate OpenAPI operationId: %s", value)
                    .isTrue();
        }
    }
}
