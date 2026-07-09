package org.plishka.backend.openapi.operation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class OperationDocumentationRegistry {
    private static final String PRODUCT_NOT_FOUND = "Product with ID 123 not found";
    private static final String CATEGORY_NOT_FOUND = "Category with ID 123 not found";
    private static final String REVIEW_NOT_FOUND = "Review with ID 123 not found";
    private static final String USER_NOT_FOUND = "User with ID 123 not found";
    private static final String ABOUT_PAGE_NOT_FOUND = "About page content not found";
    private static final String CONTACTS_PAGE_NOT_FOUND = "Contacts page content not found";
    private static final String HOME_PAGE_NOT_FOUND = "Home page content not found";
    private static final String SETTINGS_NOT_FOUND = "System settings not found";
    private static final String CART_NOT_FOUND = "Cart not found for user id=123";

    private final Map<String, OperationDocumentation> docs = buildDocs();

    public OperationDocumentation get(String operationId) {
        return docs.getOrDefault(operationId, OperationDocumentation.defaults(operationId));
    }

    public Set<String> registeredOperationIds() {
        return docs.keySet();
    }

    private Map<String, OperationDocumentation> buildDocs() {
        Map<String, OperationDocumentation> result = new LinkedHashMap<>();

        put(result, op("registerUser")
                .badRequest(validationBody(
                        "email: Email must be a valid email address",
                        "password: Password must be between 8 and 64 characters long"
                ))
                .conflict(ConflictExample.EMAIL_ALREADY_EXISTS));
        put(result, op("verifyEmail")
                .badRequest(validationAndBusiness(false, "Verification token not found",
                        "token: Email action token has invalid format")));
        put(result, op("verifyEmailChange")
                .badRequest(validationAndBusiness(true, "Email change token not found",
                        "token: Email action token has invalid format"))
                .conflict(ConflictExample.EMAIL_ALREADY_EXISTS));
        put(result, op("resendVerificationEmail")
                .badRequest(validationBody("email: Email must be a valid email address")));
        put(result, op("forgotPassword")
                .badRequest(validationBody("email: Email must be a valid email address")));
        put(result, op("resetPassword")
                .badRequest(validationAndBusiness(true, "Password reset token not found",
                        "token: Email action token has invalid format")));
        put(result, op("login")
                .loginFailureResponses()
                .badRequest(validationBody(
                        "email: Email must be a valid email address",
                        "password: Password must be between 8 and 64 characters long"
                )));
        put(result, op("refreshToken")
                .refreshFailureResponses()
                .badRequest(validationBody(
                        "refreshToken: Refresh token has invalid format",
                        "Device-Id: must be a valid UUID"
                )));
        put(result, secured("logout")
                .badRequest(validation(false, "Device-Id: must be a valid UUID")));

        put(result, secured("getCurrentUser"));
        put(result, secured("updateCurrentUser")
                .badRequest(validationBody(
                        "name: Name must be between 2 and 50 characters long",
                        "phone: Phone must be a valid Ukrainian phone number"
                )));
        put(result, secured("requestEmailChange")
                .badRequest(validationAndBusiness(true, "New email must be different from current email",
                        "newEmail: Email must be a valid email address",
                        "currentPassword: Current password is required"))
                .conflict(ConflictExample.EMAIL_ALREADY_EXISTS));
        put(result, secured("changePassword")
                .badRequest(validationAndBusiness(true, "Current password is incorrect",
                        "currentPassword: Current password is required",
                        "newPassword: New password must be between 8 and 64 characters long")));
        put(result, secured("deleteAccount")
                .badRequest(validationAndBusiness(true, "Current password is incorrect",
                        "currentPassword: Current password is required")));

        put(result, shopMode("getCart"));
        put(result, shopMode("addCartItem")
                .badRequest(validationAndBusiness(true, "Maximum quantity per item is 50",
                        "productId: Product ID must be positive",
                        "quantity: Maximum quantity per item is 50"))
                .notFound(PRODUCT_NOT_FOUND));
        put(result, shopMode("updateCartItem")
                .badRequest(validationAndBusiness(true, "Maximum quantity per item is 50",
                        "productId: must be greater than 0"))
                .notFound(PRODUCT_NOT_FOUND));
        put(result, shopMode("removeCartItem")
                .badRequest(validation(false, "productId: must be greater than 0"))
                .notFound(PRODUCT_NOT_FOUND));
        put(result, shopMode("clearCart")
                .notFound(CART_NOT_FOUND));
        put(result, shopMode("mergeCart")
                .badRequest(validationAndBusiness(true, "Maximum quantity per item is 50",
                        "items: Items list must not be empty",
                        "items[0].quantity: Maximum quantity per item is 50"))
                .notFound(PRODUCT_NOT_FOUND));

        put(result, secured("getUserOrders")
                .badRequest(validation(false, "page: Page must be >= 0", "size: Size must be <= 100")));
        put(result, secured("getOrder")
                .badRequest(validation(false, "orderId: must be greater than 0"))
                .notFound("Order with ID 123 not found"));
        put(result, secured("getOrderByNumber")
                .notFound("Order with number ORD-20260706-0001 not found"));
        put(result, shopMode("createOrder")
                .badRequest(validationAndBusiness(true, "Cart is empty, cannot proceed with checkout",
                        "Idempotency-Key: Idempotency-Key header must not exceed 64 characters",
                        "customerName: Customer name is required",
                        "deliveryCity: Delivery city is required"))
                .notFound(PRODUCT_NOT_FOUND)
                .conflict(ConflictExample.IDEMPOTENCY_CONFLICT));
        put(result, shopMode("repeatOrder")
                .badRequest(validationAndBusiness(false, "Idempotency-Key must not be blank",
                        "orderId: must be greater than 0",
                        "Idempotency-Key: Idempotency-Key header must not exceed 64 characters"))
                .notFound("Order with ID 123 not found")
                .conflict(ConflictExample.IDEMPOTENCY_CONFLICT));

        put(result, secured("getFavorites")
                .badRequest(validation(false, "page: Page must be >= 0", "size: Size must be <= 100")));
        put(result, secured("addFavorite")
                .badRequest(validation(false, "productId: must be greater than 0"))
                .notFound(PRODUCT_NOT_FOUND));
        put(result, secured("deleteFavorite")
                .badRequest(validation(false, "productId: must be greater than 0"))
                .notFound(PRODUCT_NOT_FOUND));

        put(result, secured("createCallbackRequest")
                .badRequest(validationBody(
                        "name: Name must be between 2 and 50 characters long",
                        "phone: Phone must be a valid Ukrainian phone number",
                        "message: Message must not exceed 300 characters"
                )));
        put(result, secured("getCallbackRequests")
                .badRequest(validation(false, "page: Page must be >= 0", "size: Size must be <= 100")));

        put(result, op("getProducts")
                .badRequest(validationAndBusiness(false,
                        "Price sorting is not available when shop mode is disabled",
                        "page: Page must be >= 0",
                        "size: Size must be <= 100",
                        "categoryIds: must be greater than 0",
                        "sort: Unsupported product sort"))
                .successExample(SuccessExample.PRODUCTS_PAGE));
        put(result, op("getProduct")
                .badRequest(validation(false, "id: must be greater than 0"))
                .notFound(PRODUCT_NOT_FOUND)
                .successExample(SuccessExample.PRODUCT_DETAILS));
        put(result, op("getRelatedProducts")
                .badRequest(validation(false, "id: must be greater than 0"))
                .notFound(PRODUCT_NOT_FOUND)
                .successExample(SuccessExample.RELATED_PRODUCTS_PAGE));
        put(result, secured("recordProductView")
                .badRequest(validation(false, "id: must be greater than 0"))
                .notFound(PRODUCT_NOT_FOUND));
        put(result, secured("getViewedProducts"));

        put(result, op("getReviews")
                .badRequest(validation(false, "page: Page must be >= 0", "size: Size must be <= 100")));
        put(result, op("getReview")
                .badRequest(validation(false, "id: must be greater than 0"))
                .notFound(REVIEW_NOT_FOUND));
        put(result, op("getHomePage")
                .successExample(SuccessExample.HOME_PAGE));
        put(result, op("presignDownload")
                .badRequest(validationAndBusiness(true, "S3 key format is invalid", "s3Key: S3 key is required"))
                .notFound("Media file was not found")
                .storageUnavailable());

        put(result, secured("adminGetAboutPage")
                .notFound(ABOUT_PAGE_NOT_FOUND));
        put(result, secured("adminUpdateAboutPage")
                .badRequest(validationBody(
                        "mainTitle: Main title is required",
                        "mainSubtitle: Main subtitle is required",
                        "secondaryTitle: Secondary title is required",
                        "secondarySubtitle: Secondary subtitle is required"
                ))
                .notFound(ABOUT_PAGE_NOT_FOUND));
        put(result, secured("adminAttachAboutMedia")
                .badRequest(validationAndBusiness(true, "S3 key does not match media target",
                        "s3Key: S3 key is required"))
                .notFound(ABOUT_PAGE_NOT_FOUND)
                .storageUnavailable());
        put(result, secured("adminReorderAboutMedia")
                .badRequest(validationAndBusiness(true, "Media order must contain the current media ids",
                        "mediaIds: Media ids are required",
                        "mediaIds[0]: must be greater than 0"))
                .notFound(ABOUT_PAGE_NOT_FOUND));
        put(result, secured("adminDeleteAllAboutMedia")
                .notFound(ABOUT_PAGE_NOT_FOUND));
        put(result, secured("adminDeleteAboutMedia")
                .badRequest(validation(false, "mediaId: must be greater than 0"))
                .notFound("About page media with ID 456 not found"));

        put(result, secured("adminGetCategories")
                .badRequest(validation(false, "search: Search must contain at most 100 characters")));
        put(result, secured("adminCreateCategory")
                .badRequest(validationBody("name: Category name is required"))
                .conflict(ConflictExample.CATEGORY_ALREADY_EXISTS));
        put(result, secured("adminUpdateCategory")
                .badRequest(validationBody("name: Category name is required"))
                .notFound(CATEGORY_NOT_FOUND)
                .conflict(ConflictExample.CATEGORY_ALREADY_EXISTS));
        put(result, secured("adminDeleteCategory")
                .badRequest(validationAndBusiness(false,
                        "targetCategoryId is required when strategy is MOVE_PRODUCTS",
                        "id: must be greater than 0",
                        "strategy: Required request parameter is missing",
                        "targetCategoryId: must be greater than 0"))
                .notFound(CATEGORY_NOT_FOUND));

        put(result, secured("adminGetContactsPage")
                .notFound(CONTACTS_PAGE_NOT_FOUND));
        put(result, secured("adminUpdateContactsPage")
                .badRequest(validationBody(
                        "phone: Phone must be a valid Ukrainian phone number",
                        "email: Email must be a valid email address"
                ))
                .notFound(CONTACTS_PAGE_NOT_FOUND));
        put(result, secured("adminCreateContactSocialLink")
                .badRequest(validationAndBusiness(true, "Maximum social links count is 10",
                        "name: Social link name is required",
                        "url: URL must be a valid HTTPS URL"))
                .notFound(CONTACTS_PAGE_NOT_FOUND));
        put(result, secured("adminUpdateContactSocialLink")
                .badRequest(validationAndBusiness(true, "Maximum social links count is 10",
                        "name: Social link name is required",
                        "url: URL must be a valid HTTPS URL"))
                .notFound("Contacts page social link with ID 123 not found"));
        put(result, secured("adminDeleteContactSocialLink")
                .badRequest(validation(false, "id: must be greater than 0"))
                .notFound("Contacts page social link with ID 123 not found"));

        put(result, secured("adminPresignUpload")
                .badRequest(validationAndBusiness(true, "Content type is not allowed",
                        "targetId: Target id must be greater than 0",
                        "contentType: Content type is required",
                        "checksumSha256Base64: Base64 SHA-256 checksum must be 44 characters long"))
                .notFound("Upload target with ID 123 not found")
                .storageUnavailable()
                .requestExample(RequestExample.PRESIGN_UPLOAD)
                .successExample(SuccessExample.PRESIGN_UPLOAD));

        put(result, secured("adminGetHomePage")
                .notFound(HOME_PAGE_NOT_FOUND));
        put(result, secured("adminUpdateHomePage")
                .badRequest(validationBody(
                        "title: Home page title is required",
                        "description: Home page description must contain at most 5000 characters"
                ))
                .notFound(HOME_PAGE_NOT_FOUND));

        put(result, secured("adminGetProducts")
                .badRequest(validationAndBusiness(false, "Unsupported product sort",
                        "page: Page must be >= 0",
                        "size: Size must be <= 100",
                        "categoryIds: must be greater than 0",
                        "search: Search must contain at most 100 characters")));
        put(result, secured("adminGetProduct")
                .badRequest(validation(false, "id: must be greater than 0"))
                .notFound(PRODUCT_NOT_FOUND));
        put(result, secured("adminCreateProduct")
                .badRequest(validationAndBusiness(true, "Product price must be greater than 0",
                        "name: Product name is required",
                        "price: Product price must be greater than 0",
                        "categoryId: must be greater than 0"))
                .notFound(CATEGORY_NOT_FOUND)
                .conflict(ConflictExample.PRODUCT_ALREADY_EXISTS));
        put(result, secured("adminUpdateProduct")
                .badRequest(validationAndBusiness(true, "Product price must be greater than 0",
                        "name: Product name is required",
                        "price: Product price must be greater than 0",
                        "categoryId: must be greater than 0"))
                .notFound(PRODUCT_NOT_FOUND)
                .conflict(ConflictExample.PRODUCT_ALREADY_EXISTS));
        put(result, secured("adminDeleteProduct")
                .badRequest(validation(false, "id: must be greater than 0"))
                .notFound(PRODUCT_NOT_FOUND));
        put(result, secured("adminBulkDeleteProducts")
                .badRequest(validationAndBusiness(true,
                        "Product ids are required for SELECTED selection mode",
                        "selectionMode: Selection mode is required",
                        "productIds[0]: must be greater than 0"))
                .notFound(PRODUCT_NOT_FOUND));
        put(result, secured("adminBulkUpdateProductPrices")
                .badRequest(validationAndBusiness(true, "Price value must be greater than 0",
                        "selectionMode: Selection mode is required",
                        "operation: Price operation is required",
                        "value: Price value must be greater than 0"))
                .notFound(PRODUCT_NOT_FOUND));
        put(result, secured("adminBulkUpdateProductCategories")
                .badRequest(validationAndBusiness(true, "Target category is required",
                        "selectionMode: Selection mode is required",
                        "categoryId: must be greater than 0"))
                .notFound(CATEGORY_NOT_FOUND));
        put(result, secured("adminReplaceHomeProducts")
                .badRequest(validationAndBusiness(true,
                        "Products without category cannot be shown on the home page",
                        "productIds: Home products must contain at most 100 ids",
                        "productIds[0]: must be greater than 0"))
                .notFound(PRODUCT_NOT_FOUND));
        put(result, secured("adminReorderHomeProducts")
                .badRequest(validationAndBusiness(true,
                        "Home product order must contain the current home product ids",
                        "productIds: Home products must contain at most 100 ids",
                        "productIds[0]: must be greater than 0"))
                .notFound(PRODUCT_NOT_FOUND));
        put(result, secured("adminAttachProductMedia")
                .badRequest(validationAndBusiness(true, "S3 key does not match media target",
                        "id: must be greater than 0",
                        "s3Key: S3 key is required"))
                .notFound(PRODUCT_NOT_FOUND)
                .storageUnavailable());
        put(result, secured("adminDeleteProductMedia")
                .badRequest(validation(false,
                        "productId: must be greater than 0",
                        "mediaId: must be greater than 0"))
                .notFound("Product media with ID 456 not found"));
        put(result, secured("adminDeleteAllProductMedia")
                .badRequest(validation(false, "productId: must be greater than 0"))
                .notFound(PRODUCT_NOT_FOUND));
        put(result, secured("adminMarkProductMediaPrimary")
                .badRequest(validationAndBusiness(false, "Only IMAGE media can be primary",
                        "productId: must be greater than 0",
                        "mediaId: must be greater than 0"))
                .notFound("Product media with ID 456 not found"));

        put(result, secured("adminGetReviews")
                .badRequest(validation(false, "page: Page must be >= 0", "size: Size must be <= 100")));
        put(result, secured("adminGetReview")
                .badRequest(validation(false, "id: must be greater than 0"))
                .notFound(REVIEW_NOT_FOUND));
        put(result, secured("adminCreateReview")
                .badRequest(validationBody(
                        "authorName: Review author name is required",
                        "content: Review content is required"
                )));
        put(result, secured("adminUpdateReview")
                .badRequest(validationBody(
                        "authorName: Review author name is required",
                        "content: Review content is required"
                ))
                .notFound(REVIEW_NOT_FOUND));
        put(result, secured("adminDeleteReview")
                .badRequest(validation(false, "id: must be greater than 0"))
                .notFound(REVIEW_NOT_FOUND));
        put(result, secured("adminUpdateReviewFeatured")
                .badRequest(validationAndBusiness(true,
                        "Featured reviews are limited to a maximum of 5",
                        "id: must be greater than 0",
                        "featured: must not be null"))
                .notFound(REVIEW_NOT_FOUND));
        put(result, secured("adminAttachReviewMedia")
                .badRequest(validationAndBusiness(true, "S3 key does not match media target",
                        "id: must be greater than 0",
                        "s3Key: S3 key is required"))
                .notFound(REVIEW_NOT_FOUND)
                .storageUnavailable());
        put(result, secured("adminDeleteReviewMedia")
                .badRequest(validation(false,
                        "reviewId: must be greater than 0",
                        "mediaId: must be greater than 0"))
                .notFound("Review media with ID 456 not found"));
        put(result, secured("adminDeleteAllReviewMedia")
                .badRequest(validation(false, "reviewId: must be greater than 0"))
                .notFound(REVIEW_NOT_FOUND));
        put(result, secured("adminMarkReviewMediaPrimary")
                .badRequest(validationAndBusiness(false, "Only IMAGE media can be primary",
                        "reviewId: must be greater than 0",
                        "mediaId: must be greater than 0"))
                .notFound(REVIEW_NOT_FOUND));

        put(result, secured("adminGetSettings")
                .notFound(SETTINGS_NOT_FOUND));
        put(result, secured("adminUpdateSettings")
                .badRequest(validationBody("isShopModeEnabled: must not be null"))
                .notFound(SETTINGS_NOT_FOUND));

        put(result, secured("adminGetUsers")
                .badRequest(validationAndBusiness(false, "Unsupported user sort",
                        "page: Page must be >= 0",
                        "size: Size must be <= 100",
                        "search: Search must contain at most 100 characters",
                        "sort: Unsupported user sort")));
        put(result, secured("adminGetBannedUsers")
                .badRequest(validationAndBusiness(false, "Unsupported user sort",
                        "page: Page must be >= 0",
                        "size: Size must be <= 100",
                        "search: Search must contain at most 100 characters",
                        "sort: Unsupported user sort")));
        put(result, secured("adminBanUser")
                .badRequest(validation(false, "id: must be greater than 0"))
                .notFound(USER_NOT_FOUND));
        put(result, secured("adminUnbanUser")
                .badRequest(validation(false, "id: must be greater than 0"))
                .notFound(USER_NOT_FOUND));
        put(result, secured("adminBulkBanUsers")
                .badRequest(validationBody(
                        "userIds: User ids are required",
                        "userIds[0]: must be greater than 0"
                ))
                .notFound(USER_NOT_FOUND));
        put(result, secured("adminBulkUnbanUsers")
                .badRequest(validationBody(
                        "userIds: User ids are required",
                        "userIds[0]: must be greater than 0"
                ))
                .notFound(USER_NOT_FOUND));

        return Map.copyOf(result);
    }

    private static void put(Map<String, OperationDocumentation> docs, OperationDocumentation.Builder builder) {
        OperationDocumentation doc = builder.build();
        OperationDocumentation previous = docs.putIfAbsent(doc.operationId(), doc);
        if (previous != null) {
            throw new IllegalStateException("Duplicate OpenAPI operation documentation: " + doc.operationId());
        }
    }

    private static OperationDocumentation.Builder op(String operationId) {
        return OperationDocumentation.builder(operationId);
    }

    private static OperationDocumentation.Builder secured(String operationId) {
        return op(operationId).secured();
    }

    private static OperationDocumentation.Builder shopMode(String operationId) {
        return op(operationId).shopMode();
    }

    private static BadRequestDocumentation validation(boolean requestBody, String... fieldErrors) {
        return BadRequestDocumentation.validation(requestBody, fieldErrors);
    }

    private static BadRequestDocumentation validationBody(String... fieldErrors) {
        return BadRequestDocumentation.validation(true, fieldErrors);
    }

    private static BadRequestDocumentation validationAndBusiness(
            boolean requestBody,
            String businessMessage,
            String... fieldErrors
    ) {
        return BadRequestDocumentation.validationAndBusiness(requestBody, businessMessage, fieldErrors);
    }
}
