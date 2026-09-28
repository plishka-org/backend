package org.plishka.backend.exception;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.plishka.backend.dto.common.ErrorResponseDto;
import org.plishka.backend.monitoring.sentry.SentryMonitoringService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {
    @Test
    void handleUnexpectedException_ShouldReturn500AndCaptureOperationalSettingsFailure() {
        SentryMonitoringService sentryMonitoringService = mock(SentryMonitoringService.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<SentryMonitoringService> sentryMonitoringServiceProvider = mock(ObjectProvider.class);
        when(sentryMonitoringServiceProvider.getIfAvailable()).thenReturn(sentryMonitoringService);
        GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler(
                Clock.fixed(Instant.parse("2026-07-22T12:00:00Z"), ZoneOffset.UTC),
                sentryMonitoringServiceProvider
        );
        RequiredSingletonUnavailableException exception =
                new RequiredSingletonUnavailableException("System settings not found");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/orders");

        ResponseEntity<ErrorResponseDto> response = exceptionHandler.handleUnexpectedException(exception, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("An unexpected error occurred", response.getBody().message());
        verify(sentryMonitoringService).captureException(exception, "api", "unexpected_exception");
    }
}
