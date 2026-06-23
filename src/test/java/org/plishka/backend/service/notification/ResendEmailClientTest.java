package org.plishka.backend.service.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.plishka.backend.config.properties.ResendProperties;
import org.plishka.backend.exception.InvalidEmailRecipientException;
import org.plishka.backend.service.notification.email.transport.resend.ResendEmailClient;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class ResendEmailClientTest {
    private static final String SUBJECT = "Subject";

    @Mock
    private ObjectMapper objectMapper;

    private ResendEmailClient resendEmailClient;

    @BeforeEach
    void setUp() {
        resendEmailClient = new ResendEmailClient(
                objectMapper,
                new ResendProperties("test-api-key", "Plishka <noreply@example.com>")
        );
    }

    @Test
    void send_ShouldRejectBlankRecipient() {
        InvalidEmailRecipientException exception = assertThrows(
                InvalidEmailRecipientException.class,
                () -> resendEmailClient.sendEmail("  ", SUBJECT, "text")
        );

        assertEquals("Email recipient must not be blank", exception.getMessage());
    }

    @Test
    void send_ShouldRejectNullRecipient() {
        assertThrows(
                InvalidEmailRecipientException.class,
                () -> resendEmailClient.sendEmail(null, SUBJECT, "text")
        );
    }
}
