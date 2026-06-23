package org.plishka.backend.service.notification.email;

import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.plishka.backend.config.properties.BackendProperties;
import org.plishka.backend.event.callback.CallbackRequestCreatedEvent;
import org.plishka.backend.event.order.OrderCreatedEvent;
import org.plishka.backend.service.notification.email.transport.AsyncEmailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    private final AsyncEmailSender asyncEmailSender;
    private final EmailDisplayFormatter displayFormatter;
    private final BackendProperties backendProperties;

    public void sendEmailVerificationEmail(String email, String verificationLink) {
        queueEmail(
                email,
                "Підтвердження email",
                buildEmailVerificationText(verificationLink)
        );
    }

    public void sendPasswordResetEmail(String email, String resetPasswordLink) {
        queueEmail(
                email,
                "Відновлення пароля",
                buildPasswordResetEmailText(resetPasswordLink)
        );
    }

    public void sendEmailChangeVerificationEmail(String email, String verificationLink) {
        queueEmail(
                email,
                "Підтвердження нової email-адреси",
                buildEmailChangeVerificationText(verificationLink)
        );
    }

    public void sendEmailChangedNotificationEmail(String oldEmail, String newEmail) {
        queueEmail(
                oldEmail,
                "Email вашого акаунта змінено",
                buildEmailChangedNotificationText(newEmail)
        );
    }

    public void sendOrderCreatedNotifications(OrderCreatedEvent event) {
        String text = buildOrderEmailText(event);

        queueEmail(
                event.userEmail(),
                "Ваше замовлення %s".formatted(event.orderNumber()),
                text
        );
        queueEmail(
                backendProperties.admin().email(),
                "Нове замовлення %s".formatted(event.orderNumber()),
                buildOrderAdminEmailText(event)
        );
    }

    public void sendCallbackCreatedNotifications(CallbackRequestCreatedEvent event) {
        queueEmail(
                event.userEmail(),
                "Ми отримали вашу заявку на дзвінок",
                buildCallbackConfirmationUserText(event)
        );
        queueEmail(
                backendProperties.admin().email(),
                "Нова заявка на дзвінок",
                buildCallbackNotificationAdminText(event)
        );
    }

    private void queueEmail(String to, String subject, String text) {
        try {
            asyncEmailSender.sendEmailAsync(to, subject, text);
        } catch (RejectedExecutionException exception) {
            log.error(
                    "Email task rejected because executor is saturated: to={}, subject={}",
                    to,
                    subject,
                    exception
            );
        }
    }

    private String buildEmailVerificationText(String verificationLink) {
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

    private String buildPasswordResetEmailText(String resetPasswordLink) {
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

    private String buildEmailChangeVerificationText(String verificationLink) {
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

    private String buildEmailChangedNotificationText(String newEmail) {
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

    private String buildOrderEmailText(OrderCreatedEvent event) {
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
                        event.totalPrice().toPlainString()
                );
    }

    private String buildOrderAdminEmailText(OrderCreatedEvent event) {
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
                        event.totalPrice().toPlainString()
                );
    }

    private String buildCallbackConfirmationUserText(CallbackRequestCreatedEvent event) {
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

    private String buildCallbackNotificationAdminText(CallbackRequestCreatedEvent event) {
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
                        item.unitPrice().toPlainString(),
                        item.lineTotal().toPlainString()
                ))
                .collect(Collectors.joining("\n"));
    }

    private String formatTtl(java.time.Duration duration) {
        return displayFormatter.formatDurationHours(duration);
    }
}
