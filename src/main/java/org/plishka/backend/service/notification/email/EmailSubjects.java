package org.plishka.backend.service.notification.email;

public final class EmailSubjects {
    public static final String EMAIL_VERIFICATION = "Підтвердження email";
    public static final String PASSWORD_RESET = "Відновлення пароля";
    public static final String EMAIL_CHANGE_VERIFICATION = "Підтвердження нової email-адреси";
    public static final String EMAIL_CHANGED = "Email вашого акаунта змінено";
    public static final String CALLBACK_CONFIRMATION_USER = "Ми отримали вашу заявку на дзвінок";
    public static final String CALLBACK_NOTIFICATION_ADMIN = "Нова заявка на дзвінок";

    private static final String ORDER_CONFIRMATION_USER_TEMPLATE = "Ваше замовлення %s";
    private static final String ORDER_NOTIFICATION_ADMIN_TEMPLATE = "Нове замовлення %s";

    private EmailSubjects() {
    }

    public static String orderConfirmationUser(String orderNumber) {
        return ORDER_CONFIRMATION_USER_TEMPLATE.formatted(orderNumber);
    }

    public static String orderNotificationAdmin(String orderNumber) {
        return ORDER_NOTIFICATION_ADMIN_TEMPLATE.formatted(orderNumber);
    }
}
