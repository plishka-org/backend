package org.plishka.backend.controller.user;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.plishka.backend.domain.user.User;
import org.plishka.backend.repository.user.UserRepository;
import org.plishka.backend.security.AuthenticatedUserPrincipal;
import org.plishka.backend.testsupport.MySqlIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserAccountDeletionIntegrationTest extends MySqlIntegrationTest {
    private static final String CURRENT_PASSWORD = "OldPassword123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void deleteAccount_ShouldDeleteUserAndKeepOrderWithNullUserReference() throws Exception {
        User user = createUser();
        String orderNumber = createOrder(user.getId());

        mockMvc.perform(delete("/users/me")
                        .with(authenticatedUser(user.getId(), user.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "%s"
                                }
                                """.formatted(CURRENT_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Account deleted successfully."));

        assertFalse(userRepository.existsById(user.getId()));
        assertNull(findOrderUserId(orderNumber));
        assertEquals("Deleted Account User", findOrderCustomerName(orderNumber));
    }

    private User createUser() {
        User user = new User();
        user.setName("Account Delete User");
        user.setEmail("delete-account-" + System.nanoTime() + "@example.com");
        user.setPhone("+380501234567");
        user.setPasswordHash(passwordEncoder.encode(CURRENT_PASSWORD));
        user.setEmailVerified(true);
        return userRepository.saveAndFlush(user);
    }

    private String createOrder(Long userId) {
        String orderNumber = "DELETE-ACCOUNT-" + System.nanoTime();
        jdbcTemplate.update(
                """
                        insert into orders
                            (user_id, order_number, idempotency_key, request_hash, customer_name, total_price,
                             delivery_city, phone, notes, created_at, updated_at)
                        values (?, ?, ?, ?, 'Deleted Account User', 100, 'Kyiv', '+380501234567', null,
                                current_timestamp(6), current_timestamp(6))
                        """,
                userId,
                orderNumber,
                "delete-account-" + System.nanoTime(),
                "0123456789012345678901234567890123456789012345678901234567890123"
        );
        return orderNumber;
    }

    private Long findOrderUserId(String orderNumber) {
        return jdbcTemplate.queryForObject(
                "select user_id from orders where order_number = ?",
                Long.class,
                orderNumber
        );
    }

    private String findOrderCustomerName(String orderNumber) {
        return jdbcTemplate.queryForObject(
                "select customer_name from orders where order_number = ?",
                String.class,
                orderNumber
        );
    }

    private static RequestPostProcessor authenticatedUser(Long userId, String email) {
        AuthenticatedUserPrincipal principal = AuthenticatedUserPrincipal.builder()
                .userId(userId)
                .email(email)
                .passwordHash("hash")
                .enabled(true)
                .accountNonLocked(true)
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                .build();

        return authentication(new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        ));
    }
}
