package org.plishka.backend.monitoring.sentry;

import io.sentry.Breadcrumb;
import io.sentry.Hint;
import io.sentry.SentryEvent;
import io.sentry.SentryOptions;
import io.sentry.protocol.Request;
import io.sentry.protocol.User;
import org.junit.jupiter.api.Test;
import org.plishka.backend.exception.BadRequestException;
import org.plishka.backend.exception.RequiredSingletonUnavailableException;
import org.plishka.backend.exception.ResourceNotFoundException;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class SentryPrivacyConfigTest {
    private final SentryPrivacyConfig sentryPrivacyConfig = new SentryPrivacyConfig();

    @Test
    void beforeSend_ShouldDropExpectedClientExceptions() {
        SentryOptions.BeforeSendCallback callback = sentryPrivacyConfig.sentryBeforeSendCallback();

        assertNull(callback.execute(new SentryEvent(new BadRequestException("bad request")), new Hint()));
        assertNull(callback.execute(new SentryEvent(new ResourceNotFoundException("not found")), new Hint()));
        assertNull(callback.execute(new SentryEvent(new NoResourceFoundException(
                HttpMethod.GET,
                "/missing",
                "classpath:/static/"
        )), new Hint()));
    }

    @Test
    void beforeSend_ShouldRemovePotentialPiiFromUnexpectedExceptions() {
        SentryOptions.BeforeSendCallback callback = sentryPrivacyConfig.sentryBeforeSendCallback();
        SentryEvent event = new SentryEvent(new RequiredSingletonUnavailableException("Required singleton missing"));
        event.setUser(new User());
        event.setRequest(new Request());
        event.addBreadcrumb(new Breadcrumb());

        SentryEvent processedEvent = callback.execute(event, new Hint());

        assertSame(event, processedEvent);
        assertNull(event.getUser());
        assertNull(event.getRequest());
        assertNull(event.getBreadcrumbs());
    }

    @Test
    void beforeBreadcrumb_ShouldDropBreadcrumbs() {
        SentryOptions.BeforeBreadcrumbCallback callback = sentryPrivacyConfig.sentryBeforeBreadcrumbCallback();

        assertNull(callback.execute(new Breadcrumb(), new Hint()));
    }
}
