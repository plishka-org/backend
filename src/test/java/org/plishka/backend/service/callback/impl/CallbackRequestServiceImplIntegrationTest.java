package org.plishka.backend.service.callback.impl;

import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.plishka.backend.domain.user.User;
import org.plishka.backend.dto.callback.CallbackRequestCreateDto;
import org.plishka.backend.dto.callback.CallbackRequestDto;
import org.plishka.backend.repository.user.UserRepository;
import org.plishka.backend.service.callback.CallbackRequestService;
import org.plishka.backend.service.notification.email.transport.resend.ResendEmailTransport;
import org.plishka.backend.testsupport.MySqlIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
class CallbackRequestServiceImplIntegrationTest extends MySqlIntegrationTest {
    private static final String PASSWORD_HASH =
            "012345678901234567890123456789012345678901234567890123456789";

    @Autowired
    private CallbackRequestService callbackRequestService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private ResendEmailTransport resendEmailTransport;

    @Test
    void createCallbackRequest_ShouldCreateAdminNotificationOutboxRow() {
        User user = createUser();

        CallbackRequestDto callbackRequest = callbackRequestService.createCallbackRequest(
                user.getId(),
                new CallbackRequestCreateDto(
                        "Callback User",
                        "+380501234567",
                        "Please call me about delivery"
                )
        );

        assertEquals(1, countAdminNotificationOutboxRows(
                "CALLBACK_CREATED",
                callbackRequest.callbackRequestId()
        ));
    }

    private User createUser() {
        User user = new User();
        user.setName("Callback User");
        user.setEmail("callback-outbox-" + System.nanoTime() + "@example.com");
        user.setPhone("+380501234567");
        user.setPasswordHash(PASSWORD_HASH);
        user.setEmailVerified(true);
        return userRepository.saveAndFlush(user);
    }

    private int countAdminNotificationOutboxRows(String notificationType, Long sourceId) {
        return Objects.requireNonNull(jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from admin_notification_outbox
                        where notification_type = ? and source_id = ?
                        """,
                Integer.class,
                notificationType,
                sourceId
        ));
    }
}
