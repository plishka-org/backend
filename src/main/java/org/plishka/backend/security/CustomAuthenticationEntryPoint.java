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
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Override
    public void commence(@NonNull HttpServletRequest request,
                         @NonNull HttpServletResponse response,
                         @NonNull AuthenticationException authException) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ErrorResponseDto errorResponse = new ErrorResponseDto(
                Instant.now(clock),
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                "Invalid or missing credentials",
                request.getRequestURI());

        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}
