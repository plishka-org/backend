package org.plishka.backend.openapi.support;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.plishka.backend.openapi.operation.OperationDocumentation;

public class OpenApiExamples {
    public static final String JSON_MEDIA_TYPE = org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

    public void addExamples(Operation operation, OperationDocumentation documentation) {
        addSuccessResponseExamples(operation, documentation);
        addRequestBodyExamples(operation, documentation);
    }

    OpenApiExample example(String name, String summary, Object value) {
        return new OpenApiExample(name, summary, value);
    }

    Map<String, Object> errorExample(int status, String error, String message, String path) {
        Map<String, Object> example = new LinkedHashMap<>();
        example.put("timestamp", OpenApiExampleValues.EXAMPLE_TIMESTAMP);
        example.put("status", status);
        example.put("error", error);
        example.put("message", message);
        example.put("path", path);
        return example;
    }

    Map<String, Object> validationErrorExample(String path, List<String> fieldErrors) {
        Map<String, Object> example = new LinkedHashMap<>(errorExample(
                400,
                "Bad Request",
                "Validation failed",
                exampleApiPath(path)
        ));
        example.put("fieldErrors", fieldErrors);
        return example;
    }

    String exampleApiPath(String path) {
        return "/api" + path
                .replace("{id}", "123")
                .replace("{productId}", "123")
                .replace("{orderId}", "123")
                .replace("{orderNumber}", "ORD-20260706-0001")
                .replace("{reviewId}", "123")
                .replace("{mediaId}", "456")
                .replace("{socialLinkId}", "123");
    }

    private void addSuccessResponseExamples(Operation operation, OperationDocumentation documentation) {
        switch (documentation.successExample()) {
            case PRODUCTS_PAGE -> addJsonResponseExample(
                    operation,
                    "200",
                    "productsPage",
                    "Products page",
                    productPageExample()
            );
            case PRODUCT_DETAILS -> addJsonResponseExample(
                    operation,
                    "200",
                    "productDetails",
                    "Product details",
                    productDetailExample()
            );
            case RELATED_PRODUCTS_PAGE -> addJsonResponseExample(
                    operation,
                    "200",
                    "relatedProductsPage",
                    "Related products page",
                    relatedProductPageExample()
            );
            case HOME_PAGE -> addJsonResponseExample(
                    operation,
                    "200",
                    "homePage",
                    "Home page",
                    homePageExample()
            );
            case PRESIGN_UPLOAD -> addJsonResponseExample(
                    operation,
                    "200",
                    "presignUpload",
                    "Presigned upload URL",
                    presignUploadResponseExample()
            );
            case NONE -> {
            }
            default -> throw new IllegalStateException(
                    "Unsupported success example: " + documentation.successExample()
            );
        }
    }

    private void addRequestBodyExamples(Operation operation, OperationDocumentation documentation) {
        if (operation.getRequestBody() == null) {
            return;
        }

        switch (documentation.requestExample()) {
            case PRESIGN_UPLOAD -> addPresignUploadRequestExample(operation);
            case NONE -> {
            }
            default -> throw new IllegalStateException(
                    "Unsupported request example: " + documentation.requestExample()
            );
        }
    }

    private void addPresignUploadRequestExample(Operation operation) {
        Content content = operation.getRequestBody().getContent();
        if (content == null) {
            content = new Content();
            operation.getRequestBody().setContent(content);
        }

        MediaType mediaType = getOrCreateJsonMediaType(content);
        mediaType.setExample(null);
        mediaType.setExamples(new LinkedHashMap<>());
        mediaType.addExamples(
                "presignUploadRequest",
                new Example()
                        .summary("Presigned upload request")
                        .value(presignUploadRequestExample())
        );
    }

    private void addJsonResponseExample(
            Operation operation,
            String responseCode,
            String exampleName,
            String summary,
            Object value
    ) {
        ApiResponse response = operation.getResponses() == null ? null : operation.getResponses().get(responseCode);
        if (response == null || response.getContent() == null) {
            return;
        }

        MediaType mediaType = response.getContent().get(JSON_MEDIA_TYPE);
        if (mediaType == null) {
            mediaType = response.getContent().get("*/*");
            if (mediaType == null) {
                return;
            }
            response.getContent().addMediaType(JSON_MEDIA_TYPE, mediaType);
        }

        mediaType.addExamples(exampleName, new Example().summary(summary).value(value));
    }

    private MediaType getOrCreateJsonMediaType(Content content) {
        MediaType mediaType = content.get(JSON_MEDIA_TYPE);
        if (mediaType == null) {
            mediaType = content.get("*/*");
        }
        if (mediaType == null) {
            mediaType = new MediaType();
        }

        content.addMediaType(JSON_MEDIA_TYPE, mediaType);
        return mediaType;
    }

    private Map<String, Object> productPageExample() {
        Map<String, Object> example = new LinkedHashMap<>();
        example.put("content", List.of(productSummaryExample(123L, OpenApiExampleValues.PRIMARY_PRODUCT_NAME)));
        example.put("pageNumber", 0);
        example.put("pageSize", 16);
        example.put("totalElements", 1);
        example.put("totalPages", 1);
        example.put("last", true);
        return example;
    }

    private Map<String, Object> relatedProductPageExample() {
        Map<String, Object> example = new LinkedHashMap<>();
        example.put("content", List.of(productSummaryExample(124L, OpenApiExampleValues.RELATED_PRODUCT_NAME)));
        example.put("pageNumber", 0);
        example.put("pageSize", 4);
        example.put("totalElements", 1);
        example.put("totalPages", 1);
        example.put("last", true);
        return example;
    }

    private Map<String, Object> productSummaryExample(Long productId, String name) {
        Map<String, Object> example = new LinkedHashMap<>();
        example.put("productId", productId);
        example.put("name", name);
        example.put("category", categoryExample());
        example.put("price", 1499);
        example.put("primaryMedia", productMediaPreviewExample(productId));
        return example;
    }

    private Map<String, Object> productDetailExample() {
        Map<String, Object> example = new LinkedHashMap<>();
        example.put("productId", 123);
        example.put("name", OpenApiExampleValues.PRIMARY_PRODUCT_NAME);
        example.put("description", OpenApiExampleValues.PRODUCT_DESCRIPTION);
        example.put("price", 1499);
        example.put("category", categoryExample());
        example.put("media", List.of(productMediaExample()));
        return example;
    }

    private Map<String, Object> homePageExample() {
        Map<String, Object> example = new LinkedHashMap<>();
        example.put("content", Map.of(
                "title", OpenApiExampleValues.HOME_TITLE,
                "description", OpenApiExampleValues.HOME_DESCRIPTION
        ));
        example.put("products", List.of(productSummaryExample(123L, OpenApiExampleValues.PRIMARY_PRODUCT_NAME)));
        example.put("featuredReviews", List.of());
        return example;
    }

    private Map<String, Object> categoryExample() {
        Map<String, Object> example = new LinkedHashMap<>();
        example.put("categoryId", 1);
        example.put("name", OpenApiExampleValues.ENDPOINT_CATEGORY_NAME);
        return example;
    }

    private Map<String, Object> productMediaPreviewExample(Long productId) {
        Map<String, Object> example = new LinkedHashMap<>();
        example.put("productMediaId", 10);
        example.put("s3Key", OpenApiExampleValues.productImageS3Key(productId));
        example.put("mediaType", OpenApiExampleValues.IMAGE_MEDIA_TYPE);
        return example;
    }

    private Map<String, Object> productMediaExample() {
        Map<String, Object> example = new LinkedHashMap<>(productMediaPreviewExample(123L));
        example.put("isPrimary", true);
        example.put("displayOrder", 1);
        return example;
    }

    private Map<String, Object> presignUploadRequestExample() {
        Map<String, Object> example = new LinkedHashMap<>();
        example.put("targetType", OpenApiExampleValues.PRODUCT_TARGET_TYPE);
        example.put("targetId", 123);
        example.put("mediaType", OpenApiExampleValues.IMAGE_MEDIA_TYPE);
        example.put("contentType", OpenApiExampleValues.IMAGE_WEBP_CONTENT_TYPE);
        example.put("sizeBytes", 1048576);
        example.put("originalFilename", OpenApiExampleValues.IMAGE_WEBP_FILENAME);
        example.put("checksumSha256Base64", OpenApiExampleValues.CHECKSUM_SHA256_BASE64);
        return example;
    }

    private Map<String, Object> presignUploadResponseExample() {
        Map<String, Object> requiredHeaders = new LinkedHashMap<>();
        requiredHeaders.put("Content-Type", OpenApiExampleValues.IMAGE_WEBP_CONTENT_TYPE);
        requiredHeaders.put("x-amz-checksum-sha256", OpenApiExampleValues.CHECKSUM_SHA256_BASE64);
        requiredHeaders.put("x-amz-tagging", OpenApiExampleValues.PENDING_UPLOAD_TAGGING);

        Map<String, Object> example = new LinkedHashMap<>();
        example.put("s3Key", OpenApiExampleValues.PRODUCT_IMAGE_S3_KEY);
        example.put("uploadUrl", OpenApiExampleValues.PRESIGNED_STORAGE_URL);
        example.put("method", "PUT");
        example.put("expiresAt", OpenApiExampleValues.PRESIGNED_URL_EXPIRES_AT);
        example.put("requiredHeaders", requiredHeaders);
        return example;
    }
}
