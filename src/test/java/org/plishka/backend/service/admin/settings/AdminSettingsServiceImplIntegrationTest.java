package org.plishka.backend.service.admin.settings;

import org.junit.jupiter.api.Test;
import org.plishka.backend.dto.admin.settings.AdminSettingsRequestDto;
import org.plishka.backend.service.notification.email.transport.resend.ResendEmailTransport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminSettingsServiceImplIntegrationTest {
    private static final long SINGLETON_ID = 1L;

    @Autowired
    private AdminSettingsService adminSettingsService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private ResendEmailTransport resendEmailTransport;

    @Test
    void settingsFlow_ShouldToggleShopMode() {
        assertFalse(adminSettingsService.getSettings().isShopModeEnabled());

        adminSettingsService.updateSettings(new AdminSettingsRequestDto(true));

        assertTrue(findShopModeEnabled());
        assertTrue(adminSettingsService.getSettings().isShopModeEnabled());
    }

    private boolean findShopModeEnabled() {
        Boolean enabled = jdbcTemplate.queryForObject(
                "select is_shop_mode_enabled from system_settings where id = ?",
                Boolean.class,
                SINGLETON_ID
        );
        return Boolean.TRUE.equals(enabled);
    }
}
