package org.plishka.backend.service.notification.email.transport;

public interface EmailTransport {
    void sendEmail(String to, String subject, String text);
}
