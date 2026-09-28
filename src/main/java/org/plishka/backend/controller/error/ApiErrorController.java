package org.plishka.backend.controller.error;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.dto.common.ErrorResponseDto;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Hidden
@RestController
@RequiredArgsConstructor
@RequestMapping("/error")
public class ApiErrorController implements ErrorController {
    private static final String NOT_FOUND_MESSAGE = "Endpoint not found";
    private static final String INTERNAL_SERVER_ERROR_MESSAGE = "An unexpected error occurred";

    private final Clock clock;

    @RequestMapping
    public ResponseEntity<ErrorResponseDto> error(HttpServletRequest request) {
        HttpStatus status = resolveStatus(request);

        ErrorResponseDto errorResponse = ErrorResponseDto.builder()
                .timestamp(Instant.now(clock))
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(resolveMessage(status))
                .path(resolvePath(request))
                .build();

        return ResponseEntity.status(status).body(errorResponse);
    }

    private HttpStatus resolveStatus(HttpServletRequest request) {
        if (request.getDispatcherType() != DispatcherType.ERROR) {
            return HttpStatus.NOT_FOUND;
        }

        Object statusCode = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (statusCode instanceof Integer code) {
            return resolveStatus(code);
        }
        if (statusCode instanceof String code) {
            try {
                return resolveStatus(Integer.parseInt(code));
            } catch (NumberFormatException exception) {
                return HttpStatus.INTERNAL_SERVER_ERROR;
            }
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private HttpStatus resolveStatus(int statusCode) {
        HttpStatus status = HttpStatus.resolve(statusCode);
        return status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String resolveMessage(HttpStatus status) {
        if (status == HttpStatus.NOT_FOUND) {
            return NOT_FOUND_MESSAGE;
        }
        if (status == HttpStatus.INTERNAL_SERVER_ERROR) {
            return INTERNAL_SERVER_ERROR_MESSAGE;
        }
        return status.getReasonPhrase();
    }

    private String resolvePath(HttpServletRequest request) {
        Object errorPath = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        if (errorPath instanceof String path && StringUtils.hasText(path)) {
            return path;
        }
        return request.getRequestURI();
    }
}
