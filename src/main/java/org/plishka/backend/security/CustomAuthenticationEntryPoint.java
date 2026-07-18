package org.plishka.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.dto.common.ErrorResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.PathContainer;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.pattern.PathPatternParser;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private static final String UNAUTHORIZED_MESSAGE = "Invalid or missing credentials";
    private static final String NOT_FOUND_MESSAGE = "Endpoint not found";

    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final List<RequestMappingHandlerMapping> requestMappingHandlerMappings;

    @Override
    public void commence(@NonNull HttpServletRequest request,
                         @NonNull HttpServletResponse response,
                         @NonNull AuthenticationException authException) throws IOException {
        HttpStatus status = hasControllerPath(request)
                ? HttpStatus.UNAUTHORIZED
                : HttpStatus.NOT_FOUND;

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ErrorResponseDto errorResponse = ErrorResponseDto.builder()
                .timestamp(Instant.now(clock))
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(resolveMessage(status))
                .path(request.getRequestURI())
                .build();

        objectMapper.writeValue(response.getWriter(), errorResponse);
    }

    private boolean hasControllerPath(HttpServletRequest request) {
        try {
            if (!ServletRequestPathUtils.hasParsedRequestPath(request)) {
                ServletRequestPathUtils.parseAndCache(request);
            }

            return requestMappingHandlerMappings.stream()
                    .flatMap(mapping -> mapping.getHandlerMethods().keySet().stream())
                    .anyMatch(mappingInfo -> matchesPath(mappingInfo, request));
        } catch (RuntimeException exception) {
            return true;
        }
    }

    private boolean matchesPath(RequestMappingInfo mappingInfo, HttpServletRequest request) {
        PathContainer path = ServletRequestPathUtils.getParsedRequestPath(request).pathWithinApplication();
        return mappingInfo.getPatternValues().stream()
                .map(PathPatternParser.defaultInstance::parse)
                .anyMatch(pattern -> pattern.matches(path));
    }

    private String resolveMessage(HttpStatus status) {
        return status == HttpStatus.NOT_FOUND ? NOT_FOUND_MESSAGE : UNAUTHORIZED_MESSAGE;
    }
}
