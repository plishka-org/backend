package org.plishka.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.dto.common.ErrorResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Override
    public void handle(@NonNull HttpServletRequest request,
                       @NonNull HttpServletResponse response,
                       @NonNull AccessDeniedException accessDeniedException) throws IOException {

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ErrorResponseDto errorResponse = new ErrorResponseDto(
                Instant.now(clock),
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                "You do not have permission to access this resource",
                request.getRequestURI());

        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}
