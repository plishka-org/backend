package org.plishka.backend.service.about;

import org.plishka.backend.dto.about.AboutPageResponse;
import org.plishka.backend.dto.file.AttachMediaRequestDto;

public interface AboutPageService {
    AboutPageResponse getAboutPageData();

    void attachMedia(AttachMediaRequestDto request);
}
