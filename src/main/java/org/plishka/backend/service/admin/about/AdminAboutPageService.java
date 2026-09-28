package org.plishka.backend.service.admin.about;

import org.plishka.backend.dto.about.AboutPageContentDto;
import org.plishka.backend.dto.admin.about.AboutMediaOrderRequestDto;
import org.plishka.backend.dto.admin.about.AdminAboutPageContentRequestDto;
import org.plishka.backend.dto.admin.about.AdminAboutPageDto;
import org.plishka.backend.dto.file.AttachMediaRequestDto;

public interface AdminAboutPageService {
    AdminAboutPageDto getAboutPage();

    AboutPageContentDto updateAboutPageContent(AdminAboutPageContentRequestDto request);

    void attachMedia(AttachMediaRequestDto request);

    void deleteMedia(Long mediaId);

    void deleteAllMedia();

    void reorderMedia(AboutMediaOrderRequestDto request);
}
