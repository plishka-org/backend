package org.plishka.backend.openapi.support;

import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.openapi.operation.BadRequestDocumentation;
import org.plishka.backend.openapi.operation.ConflictExample;
import org.plishka.backend.openapi.operation.ForbiddenMode;
import org.plishka.backend.openapi.operation.OperationDocumentation;
import org.plishka.backend.openapi.operation.UnauthorizedMode;
import org.springframework.http.HttpHeaders;

@RequiredArgsConstructor
public class OpenApiResponses {
    private final OpenApiExamples examples;

    public ApiResponse badRequestResponse(String path, OperationDocumentation documentation) {
        BadRequestDocumentation badRequest = documentation.badRequest();
        List<OpenApiExample> responseExamples = badRequestExamples(path, badRequest);

        OpenApiExample[] responseExampleArray = responseExamples.toArray(OpenApiExample[]::new);
        if (badRequest.validation() && badRequest.hasBusiness()) {
            return jsonOneOfResponse(
                    badRequestDescription(badRequest),
                    List.of("ErrorResponseDto", "ValidationErrorResponseDto"),
                    responseExampleArray
            );
        }

        if (badRequest.validation()) {
            return jsonResponse(badRequestDescription(badRequest), "ValidationErrorResponseDto", responseExampleArray);
        }

        return jsonResponse(badRequestDescription(badRequest), "ErrorResponseDto", responseExampleArray);
    }

    private List<OpenApiExample> badRequestExamples(String path, BadRequestDocumentation badRequest) {
        List<OpenApiExample> responseExamples = new ArrayList<>();
        if (badRequest.validation()) {
            responseExamples.add(examples.example(
                    "validationError",
                    "Validation error",
                    examples.validationErrorExample(path, badRequest.validationFieldErrors())
            ));
        }

        if (badRequest.requestBody()) {
            responseExamples.add(examples.example(
                    "unknownJsonField",
                    "Unknown JSON field",
                    examples.validationErrorExample(path, List.of("unexpectedField: Unknown property"))
            ));
            responseExamples.add(examples.example(
                    "malformedJson",
                    "Malformed JSON request body",
                    examples.validationErrorExample(path, List.of("Malformed JSON request body"))
            ));
        }

        if (badRequest.hasBusiness()) {
            responseExamples.add(examples.example(
                    "businessBadRequest",
                    "Business bad request",
                    examples.errorExample(
                            400,
                            "Bad Request",
                            badRequest.businessMessage(),
                            examples.exampleApiPath(path)
                    )
            ));
        }

        return responseExamples;
    }

    public ApiResponse unauthorizedResponse(String path, UnauthorizedMode unauthorizedMode) {
        return switch (unauthorizedMode) {
            case BEARER -> bearerUnauthorizedResponse(path);
            case LOGIN -> loginUnauthorizedResponse(path);
            case REFRESH -> refreshUnauthorizedResponse(path);
            case NONE -> throw new IllegalArgumentException("Unauthorized response is not configured");
        };
    }

    public ApiResponse forbiddenResponse(String path, ForbiddenMode forbiddenMode) {
        return switch (forbiddenMode) {
            case ACCESS_DENIED -> accessDeniedForbiddenResponse(path);
            case AUTHENTICATION -> authForbiddenResponse(path);
            case SHOP_MODE -> shopModeForbiddenResponse(path);
            case NONE -> throw new IllegalArgumentException("Forbidden response is not configured");
        };
    }

    public ApiResponse notFoundResponse(String path, OperationDocumentation documentation) {
        String message = documentation.notFoundMessage() == null
                ? "Resource not found"
                : documentation.notFoundMessage();
        return singleErrorResponse(
                "Requested resource was not found.",
                404,
                "Not Found",
                "resourceNotFound",
                "Resource not found",
                message,
                path
        );
    }

    public ApiResponse conflictResponse(String path, OperationDocumentation documentation) {
        ConflictExample conflict = documentation.conflictExample();
        return jsonResponse(
                "Request conflicts with the current state of the resource.",
                "ErrorResponseDto",
                examples.example(
                        conflict.exampleName(),
                        conflict.summary(),
                        examples.errorExample(
                                409,
                                "Conflict",
                                conflict.message(),
                                examples.exampleApiPath(path)
                        )
                )
        );
    }

    public ApiResponse tooManyRequestsResponse(String path) {
        return singleErrorResponse(
                "Rate limit exceeded.",
                429,
                "Too Many Requests",
                "rateLimitExceeded",
                "Rate limit exceeded",
                "Too many requests. Please try again later.",
                path
        )
                .addHeaderObject(HttpHeaders.RETRY_AFTER, new Header()
                        .description("Number of seconds to wait before retrying.")
                        .schema(new IntegerSchema().format("int64").example(60)));
    }

    public ApiResponse internalServerErrorResponse(String path) {
        return singleErrorResponse(
                "Unexpected server error.",
                500,
                "Internal Server Error",
                "unexpectedError",
                "Unexpected server error",
                "An unexpected error occurred",
                path
        );
    }

    public ApiResponse storageUnavailableResponse(String path) {
        return singleErrorResponse(
                "File storage is temporarily unavailable.",
                503,
                "Service Unavailable",
                "storageUnavailable",
                "Storage unavailable",
                "File storage is temporarily unavailable",
                path
        );
    }

    private ApiResponse bearerUnauthorizedResponse(String path) {
        return singleErrorResponse(
                "Invalid or missing Bearer JWT credentials.",
                401,
                "Unauthorized",
                "invalidOrMissingCredentials",
                "Invalid or missing credentials",
                "Invalid or missing credentials",
                path
        );
    }

    private ApiResponse accessDeniedForbiddenResponse(String path) {
        return singleErrorResponse(
                "Authenticated user is not allowed to access the resource.",
                403,
                "Forbidden",
                "accessDenied",
                "Access denied",
                "You do not have permission to access this resource",
                path
        );
    }

    private ApiResponse authForbiddenResponse(String path) {
        return jsonResponse(
                "User is not allowed to authenticate because email is not verified or the account is banned.",
                "ErrorResponseDto",
                errorResponseExample(403, "Forbidden", "emailNotVerified", "Email is not verified",
                        "Email is not verified", path),
                errorResponseExample(403, "Forbidden", "userBanned", "User is banned", "User is banned", path)
        );
    }

    private ApiResponse loginUnauthorizedResponse(String path) {
        return jsonResponse(
                "Invalid email/password credentials or failed authentication.",
                "ErrorResponseDto",
                errorResponseExample(401, "Unauthorized", "invalidCredentials", "Invalid credentials",
                        "Invalid email or password", path),
                errorResponseExample(401, "Unauthorized", "authenticationFailed", "Authentication failed",
                        "Authentication failed", path)
        );
    }

    private ApiResponse refreshUnauthorizedResponse(String path) {
        return jsonResponse(
                "Refresh token is missing, expired, not found, bound to another device, or authentication failed.",
                "ErrorResponseDto",
                errorResponseExample(401, "Unauthorized", "refreshTokenNotFound", "Refresh token not found",
                        "Refresh token not found", path),
                errorResponseExample(401, "Unauthorized", "refreshTokenExpired", "Refresh token expired",
                        "Refresh token has expired", path),
                errorResponseExample(401, "Unauthorized", "deviceMismatch", "Device mismatch",
                        "Refresh token does not belong to this device", path),
                errorResponseExample(401, "Unauthorized", "authenticationFailed", "Authentication failed",
                        "Authentication failed", path)
        );
    }

    private ApiResponse shopModeForbiddenResponse(String path) {
        return jsonResponse(
                "Authenticated user is not allowed to use this operation, or shop mode is disabled.",
                "ErrorResponseDto",
                errorResponseExample(403, "Forbidden", "shopModeDisabled", "Shop mode disabled",
                        "Shop mode is disabled", path),
                errorResponseExample(403, "Forbidden", "accessDenied", "Access denied",
                        "You do not have permission to access this resource", path)
        );
    }

    private ApiResponse singleErrorResponse(
            String description,
            int status,
            String error,
            String exampleName,
            String summary,
            String message,
            String path
    ) {
        return jsonResponse(
                description,
                "ErrorResponseDto",
                errorResponseExample(status, error, exampleName, summary, message, path)
        );
    }

    private OpenApiExample errorResponseExample(
            int status,
            String error,
            String exampleName,
            String summary,
            String message,
            String path
    ) {
        return examples.example(
                exampleName,
                summary,
                examples.errorExample(status, error, message, examples.exampleApiPath(path))
        );
    }

    private String badRequestDescription(BadRequestDocumentation badRequest) {
        if (badRequest.requestBody() && badRequest.hasBusiness()) {
            return "Validation error, malformed JSON request body, unknown JSON field, or business bad request.";
        }

        if (badRequest.requestBody()) {
            return "Validation error, malformed JSON request body, or unknown JSON field.";
        }

        if (badRequest.hasBusiness() && badRequest.validation()) {
            return "Validation error or business bad request.";
        }

        if (badRequest.hasBusiness()) {
            return "Business bad request.";
        }

        return "Validation error.";
    }

    private ApiResponse jsonResponse(String description, String schemaName, OpenApiExample... responseExamples) {
        return jsonResponse(description, new Schema<>().$ref("#/components/schemas/" + schemaName), responseExamples);
    }

    private ApiResponse jsonResponse(String description, Schema<Object> schema, OpenApiExample... responseExamples) {
        return new ApiResponse()
                .description(description)
                .content(jsonContent(schema, responseExamples));
    }

    private ApiResponse jsonOneOfResponse(
            String description,
            List<String> schemaNames,
            OpenApiExample... responseExamples
    ) {
        return jsonResponse(
                description,
                new ComposedSchema().oneOf(schemaNames.stream()
                        .map(schemaName -> new Schema<>().$ref("#/components/schemas/" + schemaName))
                        .toList()),
                responseExamples
        );
    }

    private Content jsonContent(Schema<Object> schema, OpenApiExample... responseExamples) {
        MediaType mediaType = new MediaType().schema(schema);

        for (OpenApiExample example : responseExamples) {
            mediaType.addExamples(
                    example.name(),
                    new Example()
                            .summary(example.summary())
                            .value(example.value())
            );
        }

        return new Content().addMediaType(OpenApiExamples.JSON_MEDIA_TYPE, mediaType);
    }
}
