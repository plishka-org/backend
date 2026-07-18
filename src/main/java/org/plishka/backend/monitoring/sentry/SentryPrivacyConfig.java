package org.plishka.backend.monitoring.sentry;

import io.sentry.SentryEvent;
import io.sentry.SentryOptions;
import jakarta.validation.ConstraintViolationException;
import java.util.Set;
import org.plishka.backend.exception.AuthenticationFailedException;
import org.plishka.backend.exception.BadRequestException;
import org.plishka.backend.exception.ConflictException;
import org.plishka.backend.exception.EmailAlreadyExistsException;
import org.plishka.backend.exception.EmailNotVerifiedException;
import org.plishka.backend.exception.ForbiddenException;
import org.plishka.backend.exception.InvalidPasswordResetTokenException;
import org.plishka.backend.exception.InvalidVerificationTokenException;
import org.plishka.backend.exception.RefreshTokenDeviceMismatchException;
import org.plishka.backend.exception.RefreshTokenExpiredException;
import org.plishka.backend.exception.RefreshTokenNotFoundException;
import org.plishka.backend.exception.ResourceNotFoundException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Configuration
public class SentryPrivacyConfig {
    private static final Set<Class<? extends Throwable>> EXPECTED_EXCEPTIONS = Set.of(
            AccessDeniedException.class,
            AuthenticationFailedException.class,
            BadRequestException.class,
            ConflictException.class,
            ConstraintViolationException.class,
            EmailAlreadyExistsException.class,
            EmailNotVerifiedException.class,
            ForbiddenException.class,
            HandlerMethodValidationException.class,
            HttpMessageNotReadableException.class,
            InvalidPasswordResetTokenException.class,
            InvalidVerificationTokenException.class,
            MethodArgumentNotValidException.class,
            MethodArgumentTypeMismatchException.class,
            MissingRequestHeaderException.class,
            MissingServletRequestParameterException.class,
            RefreshTokenDeviceMismatchException.class,
            RefreshTokenExpiredException.class,
            RefreshTokenNotFoundException.class,
            ResourceNotFoundException.class
    );

    @Bean
    public SentryOptions.BeforeSendCallback sentryBeforeSendCallback() {
        return (event, hint) -> {
            if (isExpectedException(event)) {
                return null;
            }

            removePotentialPii(event);
            return event;
        };
    }

    @Bean
    public SentryOptions.BeforeBreadcrumbCallback sentryBeforeBreadcrumbCallback() {
        return (breadcrumb, hint) -> null;
    }

    private boolean isExpectedException(SentryEvent event) {
        Throwable throwable = event.getThrowable();
        while (throwable != null) {
            if (isExpectedExceptionType(throwable)) {
                return true;
            }
            throwable = throwable.getCause();
        }
        return false;
    }

    private boolean isExpectedExceptionType(Throwable throwable) {
        return EXPECTED_EXCEPTIONS.stream().anyMatch(expectedType -> expectedType.isInstance(throwable));
    }

    private void removePotentialPii(SentryEvent event) {
        event.setUser(null);
        event.setRequest(null);
        event.setBreadcrumbs(null);
    }
}
