package org.plishka.backend.service.notification.email;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EmailType {
    VERIFICATION("verification"),
    PASSWORD_RESET("password_reset"),
    EMAIL_CHANGE("email_change"),
    ORDER_USER("order_user"),
    ORDER_ADMIN("order_admin"),
    CALLBACK_USER("callback_user"),
    CALLBACK_ADMIN("callback_admin");

    private final String metricValue;
}
