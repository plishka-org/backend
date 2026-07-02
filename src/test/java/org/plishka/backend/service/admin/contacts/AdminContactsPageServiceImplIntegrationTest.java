package org.plishka.backend.service.admin.contacts;

import org.junit.jupiter.api.Test;
import org.plishka.backend.dto.admin.contacts.AdminContactsPageContentRequestDto;
import org.plishka.backend.dto.admin.contacts.AdminContactsPageSocialLinkRequestDto;
import org.plishka.backend.repository.contacts.SocialLinkRepository;
import org.plishka.backend.service.notification.email.transport.resend.ResendEmailTransport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminContactsPageServiceImplIntegrationTest {
    private static final long SINGLETON_ID = 1L;

    @Autowired
    private AdminContactsPageService adminContactsPageService;

    @Autowired
    private SocialLinkRepository socialLinkRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private ResendEmailTransport resendEmailTransport;

    @Test
    void contactsPageFlow_ShouldUpdateContentAndManageSocialLinks() {
        adminContactsPageService.updateContactsPageContent(new AdminContactsPageContentRequestDto(
                "+380501234567",
                "info@plishka.com",
                "Kyiv",
                "https://maps.example"
        ));

        var createdLink = adminContactsPageService.createSocialLink(
                new AdminContactsPageSocialLinkRequestDto("Instagram", "https://instagram.com/plishka")
        );
        adminContactsPageService.updateSocialLink(
                createdLink.socialLinkId(),
                new AdminContactsPageSocialLinkRequestDto("Telegram", "https://t.me/plishka")
        );

        assertEquals("info@plishka.com", findContactsEmail());
        assertEquals("Telegram", findSocialLinkName(createdLink.socialLinkId()));
        assertEquals(1, findSocialLinkDisplayOrder(createdLink.socialLinkId()));

        adminContactsPageService.deleteSocialLink(createdLink.socialLinkId());

        assertEquals(0, socialLinkRepository.count());
    }

    private String findContactsEmail() {
        return jdbcTemplate.queryForObject(
                "select email from contacts_page where id = ?",
                String.class,
                SINGLETON_ID
        );
    }

    private String findSocialLinkName(Long socialLinkId) {
        return jdbcTemplate.queryForObject(
                "select name from social_links where id = ?",
                String.class,
                socialLinkId
        );
    }

    private Integer findSocialLinkDisplayOrder(Long socialLinkId) {
        return jdbcTemplate.queryForObject(
                "select display_order from social_links where id = ?",
                Integer.class,
                socialLinkId
        );
    }
}
