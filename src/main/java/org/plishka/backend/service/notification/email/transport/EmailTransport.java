package org.plishka.backend.service.notification.email.transport;

import org.plishka.backend.service.notification.email.EmailType;

public interface EmailTransport {
    void sendEmail(EmailType emailType, String to, String subject, String text);
}
