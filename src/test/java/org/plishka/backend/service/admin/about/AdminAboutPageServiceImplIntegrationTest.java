package org.plishka.backend.service.admin.about;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.plishka.backend.domain.about.AboutPageContent;
import org.plishka.backend.domain.about.AboutPageMedia;
import org.plishka.backend.domain.media.MediaType;
import org.plishka.backend.dto.admin.about.AboutMediaOrderRequestDto;
import org.plishka.backend.dto.admin.about.AdminAboutPageContentRequestDto;
import org.plishka.backend.repository.about.AboutPageContentRepository;
import org.plishka.backend.repository.about.AboutPageMediaRepository;
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
class AdminAboutPageServiceImplIntegrationTest {
    private static final long SINGLETON_ID = 1L;
    private static final String ABOUT_FIRST_KEY =
            "about/1/images/2026/05/550e8400-e29b-41d4-a716-446655440002.jpg";
    private static final String ABOUT_SECOND_KEY =
            "about/1/images/2026/05/550e8400-e29b-41d4-a716-446655440003.jpg";

    @Autowired
    private AdminAboutPageService adminAboutPageService;

    @Autowired
    private AboutPageContentRepository aboutPageContentRepository;

    @Autowired
    private AboutPageMediaRepository aboutPageMediaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private ResendEmailTransport resendEmailTransport;

    @Test
    void aboutPageFlow_ShouldUpdateContentReorderAndDeleteMediaWithOutbox() {
        adminAboutPageService.updateAboutPageContent(new AdminAboutPageContentRequestDto(
                "History title",
                "History text",
                "Current title",
                "Current text"
        ));

        AboutPageMedia firstMedia = createAboutMedia(ABOUT_FIRST_KEY, 1);
        AboutPageMedia secondMedia = createAboutMedia(ABOUT_SECOND_KEY, 2);

        adminAboutPageService.reorderMedia(new AboutMediaOrderRequestDto(
                List.of(secondMedia.getId(), firstMedia.getId())
        ));
        adminAboutPageService.deleteMedia(firstMedia.getId());

        assertEquals("History title", findAboutHistoryTitle());
        assertEquals(1, findAboutMediaDisplayOrder(secondMedia.getId()));
        assertEquals(0, countAboutMediaRows(firstMedia.getId()));
        assertEquals(1, countStorageDeletionOutboxRows(ABOUT_FIRST_KEY));
    }

    private AboutPageMedia createAboutMedia(String s3Key, int displayOrder) {
        AboutPageContent content = aboutPageContentRepository.findById(SINGLETON_ID).orElseThrow();
        AboutPageMedia media = new AboutPageMedia();
        media.setAboutPage(content);
        media.setS3Key(s3Key);
        media.setMediaType(MediaType.IMAGE);
        media.setDisplayOrder(displayOrder);
        return aboutPageMediaRepository.saveAndFlush(media);
    }

    private String findAboutHistoryTitle() {
        return jdbcTemplate.queryForObject(
                "select history_title from about_page_content where id = ?",
                String.class,
                SINGLETON_ID
        );
    }

    private Integer findAboutMediaDisplayOrder(Long mediaId) {
        return jdbcTemplate.queryForObject(
                "select display_order from about_page_media where id = ?",
                Integer.class,
                mediaId
        );
    }

    private Integer countAboutMediaRows(Long mediaId) {
        return jdbcTemplate.queryForObject(
                "select count(*) from about_page_media where id = ?",
                Integer.class,
                mediaId
        );
    }

    private Integer countStorageDeletionOutboxRows(String s3Key) {
        return jdbcTemplate.queryForObject(
                "select count(*) from storage_deletion_outbox where s3_key = ?",
                Integer.class,
                s3Key
        );
    }
}
