package org.plishka.backend.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.plishka.backend.dto.common.ErrorResponseDto;
import org.plishka.backend.dto.common.ValidationErrorResponseDto;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.StringUtils;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import tools.jackson.core.JacksonException;
import tools.jackson.core.TokenStreamLocation;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {
    private static final String VALIDATION_FAILED_MESSAGE = "Validation failed";
    private static final String MALFORMED_REQUEST_BODY_MESSAGE = "Malformed JSON request body";
    private static final String EMAIL_SERVICE_UNAVAILABLE_MESSAGE = "Email service is temporarily unavailable";
    private static final String INTERNAL_SERVER_ERROR_MESSAGE = "An unexpected error occurred";
    private static final String REQUIRED_HEADER_MISSING_MESSAGE = "Required header is missing";
    private static final String REQUIRED_PARAMETER_MISSING_MESSAGE = "Required request parameter is missing";
    private static final String GLOBAL_ERROR_FIELD = "global";

    private final Clock clock;

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponseDto> handleConflict(
            EmailAlreadyExistsException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler({
            AuthenticationFailedException.class,
            RefreshTokenDeviceMismatchException.class,
            RefreshTokenExpiredException.class,
            RefreshTokenNotFoundException.class
    })
    public ResponseEntity<ErrorResponseDto> handleUnauthorized(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, exception.getMessage(), request);
    }

    @ExceptionHandler({
            EmailNotVerifiedException.class,
            ForbiddenException.class
    })
    public ResponseEntity<ErrorResponseDto> handleForbidden(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, exception.getMessage(), request);
    }

    @ExceptionHandler({
            BadRequestException.class,
            InvalidVerificationTokenException.class,
            InvalidPasswordResetTokenException.class
    })
    public ResponseEntity<ErrorResponseDto> handleBadRequest(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleNotFound(
            ResourceNotFoundException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(ResendEmailException.class)
    public ResponseEntity<ErrorResponseDto> handleServiceUnavailable(
            ResendEmailException exception,
            HttpServletRequest request
    ) {
        log.warn("Email provider error while processing {}", request.getRequestURI(), exception);
        return buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE, EMAIL_SERVICE_UNAVAILABLE_MESSAGE, request);
    }

    @ExceptionHandler(StorageOperationException.class)
    public ResponseEntity<ErrorResponseDto> handleStorageUnavailable(
            StorageOperationException exception,
            HttpServletRequest request
    ) {
        log.warn("Storage provider error while processing {}", request.getRequestURI(), exception);
        return buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponseDto> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        return buildValidationErrorResponse(extractBodyFieldErrors(exception), request);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ValidationErrorResponseDto> handleHandlerMethodValidation(
            HandlerMethodValidationException exception,
            HttpServletRequest request
    ) {
        return buildValidationErrorResponse(extractMethodFieldErrors(exception), request);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ValidationErrorResponseDto> handleMissingRequestHeader(
            MissingRequestHeaderException exception,
            HttpServletRequest request
    ) {
        return buildValidationErrorResponse(
                List.of(formatValidationMessage(exception.getHeaderName(), REQUIRED_HEADER_MISSING_MESSAGE)),
                request
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ValidationErrorResponseDto> handleMissingServletRequestParameter(
            MissingServletRequestParameterException exception,
            HttpServletRequest request
    ) {
        return buildValidationErrorResponse(
                List.of(formatValidationMessage(
                        exception.getParameterName(),
                        REQUIRED_PARAMETER_MISSING_MESSAGE
                )),
                request
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ValidationErrorResponseDto> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        return buildValidationErrorResponse(extractConstraintViolationErrors(exception), request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ValidationErrorResponseDto> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        return buildValidationErrorResponse(extractRequestBodyReadErrors(exception), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleUnexpectedException(
            Exception exception,
            HttpServletRequest request
    ) {
        log.error("Unhandled exception while processing {}", request.getRequestURI(), exception);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR_MESSAGE, request);
    }

    private ResponseEntity<ErrorResponseDto> buildErrorResponse(
            HttpStatus status,
            String message,
            HttpServletRequest request
    ) {
        ErrorResponseDto errorResponse = ErrorResponseDto.builder()
                .timestamp(Instant.now(clock))
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(status).body(errorResponse);
    }

    private ResponseEntity<ValidationErrorResponseDto> buildValidationErrorResponse(
            List<String> fieldErrors,
            HttpServletRequest request
    ) {
        ValidationErrorResponseDto errorResponse = ValidationErrorResponseDto.builder()
                .timestamp(Instant.now(clock))
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(VALIDATION_FAILED_MESSAGE)
                .path(request.getRequestURI())
                .fieldErrors(fieldErrors)
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    private List<String> extractBodyFieldErrors(MethodArgumentNotValidException exception) {
        LinkedHashSet<String> fieldErrors = new LinkedHashSet<>();

        exception.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.add(
                        formatValidationMessage(error.getField(), error.getDefaultMessage())
                ));
        exception.getBindingResult().getGlobalErrors()
                .forEach(error -> fieldErrors.add(
                        formatValidationMessage(null, error.getDefaultMessage())
                ));

        return List.copyOf(fieldErrors);
    }

    private List<String> extractMethodFieldErrors(HandlerMethodValidationException exception) {
        LinkedHashSet<String> fieldErrors = new LinkedHashSet<>();

        for (ParameterValidationResult validationResult : exception.getParameterValidationResults()) {
            String parameterName = resolveParameterName(validationResult.getMethodParameter());

            if (validationResult instanceof ParameterErrors parameterErrors) {
                parameterErrors.getFieldErrors()
                        .forEach(error -> fieldErrors.add(
                                formatValidationMessage(
                                        StringUtils.hasText(error.getField()) ? error.getField() : parameterName,
                                        error.getDefaultMessage()
                                )
                        ));
                parameterErrors.getGlobalErrors()
                        .forEach(error -> fieldErrors.add(
                                formatValidationMessage(parameterName, error.getDefaultMessage())
                        ));
                continue;
            }

            validationResult.getResolvableErrors()
                    .forEach(error -> fieldErrors.add(
                            formatValidationMessage(parameterName, error.getDefaultMessage())
                    ));
        }

        exception.getCrossParameterValidationResults()
                .forEach(error -> fieldErrors.add(
                        formatValidationMessage(null, error.getDefaultMessage())
                ));

        return List.copyOf(fieldErrors);
    }

    private List<String> extractConstraintViolationErrors(ConstraintViolationException exception) {
        LinkedHashSet<String> fieldErrors = new LinkedHashSet<>();

        for (ConstraintViolation<?> violation : exception.getConstraintViolations()) {
            fieldErrors.add(formatValidationMessage(
                    extractLeafPath(violation.getPropertyPath().toString()),
                    violation.getMessage()
            ));
        }

        return List.copyOf(fieldErrors);
    }

    private List<String> extractRequestBodyReadErrors(HttpMessageNotReadableException exception) {
        Throwable rootCause = exception.getMostSpecificCause();

        return switch (rootCause) {
            case UnrecognizedPropertyException propertyException ->
                    List.of(buildJsonFieldError(propertyException, "Unknown property"));

            case InvalidFormatException formatException -> List.of(buildJsonFieldError(
                    formatException,
                    buildInvalidValueMessage(formatException)));

            case MismatchedInputException inputException -> List.of(buildJsonFieldError(
                    inputException,
                    resolveJsonErrorMessage(inputException.getOriginalMessage())));

            case StreamReadException streamReadException -> List.of(formatJsonParseMessage(
                    resolveJsonErrorMessage(streamReadException.getOriginalMessage()),
                    streamReadException.getLocation()));

            default -> List.of(MALFORMED_REQUEST_BODY_MESSAGE);
        };
    }

    private String buildJsonFieldError(JacksonException exception, String message) {
        return formatValidationMessage(resolveJsonFieldPath(exception), message);
    }

    private String buildInvalidValueMessage(InvalidFormatException exception) {
        Class<?> expectedType = exception.getTargetType();
        Object rejectedValue = exception.getValue();

        if (expectedType != null && expectedType.isEnum()) {
            String allowedValues = Arrays.stream(expectedType.getEnumConstants())
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));
            return "Must be one of: %s".formatted(allowedValues);
        }

        String expectedTypeName = expectedType != null ? expectedType.getSimpleName() : null;
        if (expectedTypeName == null && rejectedValue == null) {
            return "Invalid value";
        }

        if (expectedTypeName == null) {
            return "Invalid value '%s'".formatted(rejectedValue);
        }

        if (rejectedValue == null) {
            return "Invalid value, expected %s".formatted(expectedTypeName);
        }

        return "Invalid value '%s', expected %s".formatted(rejectedValue, expectedTypeName);
    }

    private String resolveJsonFieldPath(JacksonException exception) {
        StringBuilder pathBuilder = new StringBuilder();

        for (JacksonException.Reference pathReference : exception.getPath()) {
            String propertyName = pathReference.getPropertyName();
            if (StringUtils.hasText(propertyName)) {
                if (!pathBuilder.isEmpty()) {
                    pathBuilder.append('.');
                }
                pathBuilder.append(propertyName);
                continue;
            }

            int index = pathReference.getIndex();
            if (index >= 0) {
                pathBuilder.append('[').append(index).append(']');
            }
        }

        return StringUtils.hasText(pathBuilder) ? pathBuilder.toString() : null;
    }

    private String formatJsonParseMessage(String message, TokenStreamLocation location) {
        if (location == null || location == TokenStreamLocation.NA) {
            return message;
        }

        int line = location.getLineNr();
        int column = location.getColumnNr();
        if (line < 0 || column < 0) {
            return message;
        }

        return "%s (line %d, column %d)".formatted(message, line, column);
    }

    private String resolveJsonErrorMessage(String message) {
        return StringUtils.hasText(message) ? message : MALFORMED_REQUEST_BODY_MESSAGE;
    }

    private String formatValidationMessage(String fieldName, String message) {
        String resolvedMessage = StringUtils.hasText(message) ? message : VALIDATION_FAILED_MESSAGE;

        if (!StringUtils.hasText(fieldName) || GLOBAL_ERROR_FIELD.equals(fieldName)) {
            return resolvedMessage;
        }

        return "%s: %s".formatted(fieldName, resolvedMessage);
    }

    private String resolveParameterName(MethodParameter methodParameter) {
        RequestHeader requestHeader = methodParameter.getParameterAnnotation(RequestHeader.class);
        if (requestHeader != null) {
            String headerName = resolveAnnotationName(requestHeader.value(), requestHeader.name());
            if (headerName != null) {
                return headerName;
            }
        }

        RequestParam requestParam = methodParameter.getParameterAnnotation(RequestParam.class);
        if (requestParam != null) {
            String paramName = resolveAnnotationName(requestParam.value(), requestParam.name());
            if (paramName != null) {
                return paramName;
            }
        }

        if (StringUtils.hasText(methodParameter.getParameterName())) {
            return methodParameter.getParameterName();
        }

        return GLOBAL_ERROR_FIELD;
    }

    private String resolveAnnotationName(String value, String name) {
        if (StringUtils.hasText(value)) {
            return value;
        }

        if (StringUtils.hasText(name)) {
            return name;
        }

        return null;
    }

    private String extractLeafPath(String propertyPath) {
        if (!StringUtils.hasText(propertyPath)) {
            return GLOBAL_ERROR_FIELD;
        }

        int lastSeparatorIndex = propertyPath.lastIndexOf('.');
        if (lastSeparatorIndex < 0) {
            return propertyPath;
        }

        return propertyPath.substring(lastSeparatorIndex + 1);
    }
}
