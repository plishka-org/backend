package org.plishka.backend.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.config.properties.RateLimitProperties;
import org.plishka.backend.dto.common.ErrorResponseDto;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {
    private static final String TOO_MANY_REQUESTS_MESSAGE = "Too many requests. Please try again later.";

    private final RateLimitRuleResolver ruleResolver;
    private final RateLimitRequestBodyExtractor requestBodyExtractor;
    private final RateLimitService rateLimitService;
    private final RateLimitProperties rateLimitProperties;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        if (!rateLimitProperties.enabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        HttpServletRequest requestToUse = request;
        Map<String, String> bodyFields = Map.of();
        Set<String> requiredBodyFields = ruleResolver.requiredBodyFields(request);

        if (!requiredBodyFields.isEmpty()) {
            CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);
            requestToUse = cachedRequest;
            bodyFields = requestBodyExtractor.extract(cachedRequest.getCachedBody(), requiredBodyFields);
        }

        List<RateLimitKey> keys = ruleResolver.resolve(requestToUse, bodyFields);
        RateLimitResult result = rateLimitService.consume(keys);
        if (!result.allowed()) {
            writeRateLimitExceededResponse(requestToUse, response, result);
            return;
        }

        filterChain.doFilter(requestToUse, response);
    }

    private void writeRateLimitExceededResponse(
            HttpServletRequest request,
            HttpServletResponse response,
            RateLimitResult result
    ) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(result.retryAfterSeconds()));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ErrorResponseDto errorResponse = ErrorResponseDto.builder()
                .timestamp(Instant.now(clock))
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .error(HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase())
                .message(TOO_MANY_REQUESTS_MESSAGE)
                .path(request.getRequestURI())
                .build();

        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}
