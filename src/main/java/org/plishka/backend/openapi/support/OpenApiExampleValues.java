package org.plishka.backend.openapi.support;

public final class OpenApiExampleValues {
    public static final String EMAIL_ACTION_TOKEN = "ccbleD_cjXLlDO1GkKe8fsAaITrzBky-n61Gk46m7WQ";
    public static final String DEVICE_ID = "550e8400-e29b-41d4-a716-446655440000";

    public static final String USER_NAME = "Olena Shevchenko";
    public static final String USER_EMAIL = "olena@example.com";
    public static final String USER_PHONE = "+380501234567";
    public static final String PASSWORD = "Password1";
    public static final String NEW_PASSWORD = "NewPassword1";

    public static final String PRIMARY_PRODUCT_NAME = "Handcrafted Wooden Storage Box";
    public static final String RELATED_PRODUCT_NAME = "Oak Serving Tray";
    public static final String PRODUCT_DESCRIPTION = "Handcrafted wooden storage box for home and garden use.";
    public static final String GENERIC_CATEGORY_NAME = "Boxes";
    public static final String ENDPOINT_CATEGORY_NAME = "Wooden Boxes";
    public static final String HOME_TITLE = "Plishka";
    public static final String HOME_DESCRIPTION = "Handcrafted wooden goods for everyday home comfort.";

    public static final String PRODUCT_TARGET_TYPE = "PRODUCT";
    public static final String IMAGE_MEDIA_TYPE = "IMAGE";
    public static final String IMAGE_WEBP_CONTENT_TYPE = "image/webp";
    public static final String IMAGE_WEBP_FILENAME = "photo.webp";
    public static final String PRODUCT_IMAGE_S3_KEY =
            "products/123/images/2026/07/550e8400-e29b-41d4-a716-446655440000.webp";
    public static final String PRESIGNED_STORAGE_URL = "https://storage.example.com/presigned";
    public static final String CHECKSUM_SHA256_BASE64 = "mD7x3Jp6uG4wZpKJcY5R3eVqT8bQ0nF2sL9hA1cX6dE=";
    public static final String PENDING_UPLOAD_TAGGING = "upload-status=pending";
    public static final String EXAMPLE_TIMESTAMP = "2026-07-06T12:00:00Z";
    public static final String PRESIGNED_URL_EXPIRES_AT = "2026-07-06T12:15:00Z";

    public static final String REGISTER_REQUEST_JSON = "{\n"
            + "  \"name\": \"" + USER_NAME + "\",\n"
            + "  \"email\": \"" + USER_EMAIL + "\",\n"
            + "  \"phone\": \"" + USER_PHONE + "\",\n"
            + "  \"password\": \"" + PASSWORD + "\",\n"
            + "  \"confirmPassword\": \"" + PASSWORD + "\"\n"
            + "}";

    public static final String RESET_PASSWORD_REQUEST_JSON = "{\n"
            + "  \"token\": \"" + EMAIL_ACTION_TOKEN + "\",\n"
            + "  \"password\": \"" + NEW_PASSWORD + "\",\n"
            + "  \"confirmPassword\": \"" + NEW_PASSWORD + "\"\n"
            + "}";

    public static final String LOGIN_REQUEST_JSON = "{\n"
            + "  \"email\": \"" + USER_EMAIL + "\",\n"
            + "  \"password\": \"" + PASSWORD + "\"\n"
            + "}";

    public static final String PRESIGN_UPLOAD_REQUEST_JSON = "{\n"
            + "  \"targetType\": \"" + PRODUCT_TARGET_TYPE + "\",\n"
            + "  \"targetId\": 123,\n"
            + "  \"mediaType\": \"" + IMAGE_MEDIA_TYPE + "\",\n"
            + "  \"contentType\": \"" + IMAGE_WEBP_CONTENT_TYPE + "\",\n"
            + "  \"sizeBytes\": 1048576,\n"
            + "  \"originalFilename\": \"" + IMAGE_WEBP_FILENAME + "\",\n"
            + "  \"checksumSha256Base64\": \"" + CHECKSUM_SHA256_BASE64 + "\"\n"
            + "}";

    public static final String PRESIGN_DOWNLOAD_REQUEST_JSON = "{\n"
            + "  \"s3Key\": \"" + PRODUCT_IMAGE_S3_KEY + "\"\n"
            + "}";

    public static final String PRESIGN_UPLOAD_REQUIRED_HEADERS_JSON = "{\n"
            + "  \"Content-Type\": \"" + IMAGE_WEBP_CONTENT_TYPE + "\",\n"
            + "  \"x-amz-checksum-sha256\": \"" + CHECKSUM_SHA256_BASE64 + "\",\n"
            + "  \"x-amz-tagging\": \"" + PENDING_UPLOAD_TAGGING + "\"\n"
            + "}";

    private static final String PRODUCT_IMAGE_S3_KEY_PREFIX = "products/";
    private static final String PRODUCT_IMAGE_S3_KEY_SUFFIX =
            "/images/2026/07/550e8400-e29b-41d4-a716-446655440000.webp";

    private OpenApiExampleValues() {
    }

    public static String productImageS3Key(Long productId) {
        return PRODUCT_IMAGE_S3_KEY_PREFIX + productId + PRODUCT_IMAGE_S3_KEY_SUFFIX;
    }
}
