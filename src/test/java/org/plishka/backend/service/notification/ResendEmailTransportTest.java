package org.plishka.backend.service.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.plishka.backend.config.properties.ResendProperties;
import org.plishka.backend.exception.InvalidEmailRecipientException;
import org.plishka.backend.service.notification.email.EmailType;
import org.plishka.backend.service.notification.email.transport.resend.ResendEmailTransport;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class ResendEmailTransportTest {
    private static final String SUBJECT = "Subject";

    @Mock
    private ObjectMapper objectMapper;

    private ResendEmailTransport resendEmailTransport;

    @BeforeEach
    void setUp() {
        resendEmailTransport = new ResendEmailTransport(
                objectMapper,
                new ResendProperties("test-api-key", "Plishka <noreply@example.com>")
        );
    }

    @Test
    void sendEmail_ShouldRejectBlankRecipient() {
        InvalidEmailRecipientException exception = assertThrows(
                InvalidEmailRecipientException.class,
                () -> resendEmailTransport.sendEmail(EmailType.VERIFICATION, "  ", SUBJECT, "text")
        );

        assertEquals("Email recipient must not be blank", exception.getMessage());
    }

    @Test
    void sendEmail_ShouldRejectNullRecipient() {
        assertThrows(
                InvalidEmailRecipientException.class,
                () -> resendEmailTransport.sendEmail(EmailType.VERIFICATION, null, SUBJECT, "text")
        );
    }
}
