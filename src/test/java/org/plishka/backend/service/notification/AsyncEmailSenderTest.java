package org.plishka.backend.service.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.plishka.backend.exception.RetryableEmailException;
import org.plishka.backend.service.notification.email.transport.AsyncEmailSender;
import org.plishka.backend.service.notification.email.transport.RetryableEmailSender;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AsyncEmailSenderTest {
    private static final String USER_EMAIL = "user@example.com";
    private static final String SUBJECT = "Підтвердження email";
    private static final String TEXT = "text body";

    @Mock
    private RetryableEmailSender retryableEmailSender;

    private AsyncEmailSender asyncEmailSender;

    @BeforeEach
    void setUp() {
        asyncEmailSender = new AsyncEmailSender(retryableEmailSender);
    }

    @Test
    void sendEmailAsync_ShouldSendEmail() {
        asyncEmailSender.sendEmailAsync(USER_EMAIL, SUBJECT, TEXT);

        verify(retryableEmailSender).sendEmail(USER_EMAIL, SUBJECT, TEXT);
    }

    @Test
    void sendEmailAsync_ShouldNotPropagateSendFailures() {
        doThrow(new RetryableEmailException("provider down"))
                .when(retryableEmailSender)
                .sendEmail(USER_EMAIL, SUBJECT, TEXT);

        assertDoesNotThrow(() -> asyncEmailSender.sendEmailAsync(USER_EMAIL, SUBJECT, TEXT));
    }
}
