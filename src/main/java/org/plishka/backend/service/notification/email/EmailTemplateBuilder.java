package org.plishka.backend.service.notification.email;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.config.properties.BackendProperties;
import org.plishka.backend.event.callback.CallbackRequestCreatedEvent;
import org.plishka.backend.event.order.OrderCreatedEvent;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailTemplateBuilder {
    private final EmailDisplayFormatter displayFormatter;
    private final BackendProperties backendProperties;

    public String buildEmailVerificationText(String verificationLink) {
        return """
                Вітаємо,

                Дякуємо за реєстрацію в Plishka.

                Підтвердіть вашу email-адресу, перейшовши за посиланням:
                %s

                Посилання діє %s

                Якщо ви не створювали акаунт, проігноруйте цей лист.

                З повагою,
                Plishka
                """
                .formatted(verificationLink, formatTtl(backendProperties.auth().emailVerificationTokenTtl()));
    }

    public String buildPasswordResetEmailText(String resetPasswordLink) {
        return """
                Вітаємо,

                Ми отримали запит на відновлення пароля до вашого акаунта Plishka.

                Перейдіть за посиланням, щоб створити новий пароль:
                %s

                Посилання діє %s

                Якщо ви не надсилали цей запит, проігноруйте цей лист.

                З повагою,
                Plishka
                """
                .formatted(resetPasswordLink, formatTtl(backendProperties.auth().passwordResetTokenTtl()));
    }

    public String buildEmailChangeVerificationText(String verificationLink) {
        return """
                Вітаємо,

                Ми отримали запит на зміну email-адреси вашого акаунта Plishka.

                Підтвердіть нову адресу, перейшовши за посиланням:
                %s

                Посилання діє %s

                Якщо ви не надсилали цей запит, проігноруйте цей лист.

                З повагою,
                Plishka
                """
                .formatted(verificationLink, formatTtl(backendProperties.auth().emailVerificationTokenTtl()));
    }

    public String buildEmailChangedNotificationText(String newEmail) {
        return """
                Вітаємо,

                Email-адресу вашого акаунта Plishka змінено на:
                %s

                Якщо це зробили не ви, негайно відновіть пароль.

                З повагою,
                Plishka
                """
                .formatted(newEmail);
    }

    public String buildOrderEmailText(OrderCreatedEvent event) {
        return """
                Вітаємо, %s!

                Дякуємо за замовлення в Plishka!

                Номер замовлення: %s
                Дата: %s

                Місто доставки: %s
                Телефон: %s
                Коментар: %s

                Товари:
                %s

                Разом: %s грн

                Ми зв'яжемося з вами, якщо знадобляться додаткові уточнення.

                З повагою,
                Plishka
                """
                .formatted(
                        event.customerName(),
                        event.orderNumber(),
                        displayFormatter.formatDateTime(event.createdAt()),
                        event.deliveryCity(),
                        event.phone(),
                        displayFormatter.formatOptionalText(event.notes()),
                        buildOrderItemsText(event.items()),
                        displayFormatter.formatPrice(event.totalPrice())
                );
    }

    public String buildOrderAdminEmailText(OrderCreatedEvent event) {
        return """
                Нове замовлення

                ID замовлення: %d
                Номер замовлення: %s
                Дата: %s

                ID користувача: %s
                Email клієнта: %s
                Ім'я: %s
                Місто доставки: %s
                Телефон: %s
                Коментар: %s

                Товари:
                %s

                Разом: %s грн
                """
                .formatted(
                        event.orderId(),
                        event.orderNumber(),
                        displayFormatter.formatDateTime(event.createdAt()),
                        displayFormatter.formatUserId(event.userId()),
                        event.userEmail(),
                        event.customerName(),
                        event.deliveryCity(),
                        event.phone(),
                        displayFormatter.formatOptionalText(event.notes()),
                        buildOrderItemsText(event.items()),
                        displayFormatter.formatPrice(event.totalPrice())
                );
    }

    public String buildCallbackConfirmationUserText(CallbackRequestCreatedEvent event) {
        return """
                Вітаємо, %s!

                Ми отримали вашу заявку на зворотний дзвінок.

                Номер заявки: %d
                Дата: %s
                Телефон: %s

                Ваше повідомлення:
                %s

                Наш менеджер зв'яжеться з вами найближчим часом.

                З повагою,
                Plishka
                """
                .formatted(
                        event.name(),
                        event.callbackRequestId(),
                        displayFormatter.formatDateTime(event.createdAt()),
                        event.phone(),
                        event.message()
                );
    }

    public String buildCallbackNotificationAdminText(CallbackRequestCreatedEvent event) {
        return """
                Нова заявка на дзвінок

                Номер заявки: %d
                ID користувача: %s
                Email користувача: %s
                Дата: %s
                Ім'я: %s
                Телефон: %s

                Повідомлення:
                %s
                """
                .formatted(
                        event.callbackRequestId(),
                        displayFormatter.formatUserId(event.userId()),
                        event.userEmail(),
                        displayFormatter.formatDateTime(event.createdAt()),
                        event.name(),
                        event.phone(),
                        event.message()
                );
    }

    private String buildOrderItemsText(List<OrderCreatedEvent.Item> items) {
        if (items == null || items.isEmpty()) {
            return EmailDisplayFormatter.EMPTY_VALUE;
        }

        return items.stream()
                .map(item -> "• %s (%s) × %d — %s грн, разом %s грн".formatted(
                        item.productName(),
                        item.categoryName(),
                        item.quantity(),
                        displayFormatter.formatPrice(item.unitPrice()),
                        displayFormatter.formatPrice(item.lineTotal())
                ))
                .collect(Collectors.joining("\n"));
    }

    private String formatTtl(Duration duration) {
        return displayFormatter.formatDurationHours(duration);
    }
}
