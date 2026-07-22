package org.plishka.backend.service.admin.home;

import lombok.RequiredArgsConstructor;
import org.plishka.backend.domain.home.HomePageContent;
import org.plishka.backend.dto.admin.home.AdminHomePageContentRequestDto;
import org.plishka.backend.dto.admin.home.AdminHomePageDto;
import org.plishka.backend.dto.home.HomePageContentDto;
import org.plishka.backend.exception.RequiredSingletonUnavailableException;
import org.plishka.backend.mapper.home.HomePageMapper;
import org.plishka.backend.repository.home.HomePageContentRepository;
import org.plishka.backend.util.UserInputNormalizer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AdminHomePageServiceImpl implements AdminHomePageService {
    private static final long SINGLETON_CONTENT_ID = 1L;
    private static final String CONTENT_NOT_FOUND_MESSAGE = "Home page content not found";

    private final HomePageContentRepository contentRepository;
    private final HomePageMapper homePageMapper;

    @Override
    @Transactional(readOnly = true)
    public AdminHomePageDto getHomePage() {
        HomePageContent content = findContentOrThrow();
        return new AdminHomePageDto(homePageMapper.toContentDto(content));
    }

    @Override
    @Transactional
    public HomePageContentDto updateHomePageContent(AdminHomePageContentRequestDto request) {
        HomePageContent content = findContentForUpdateOrThrow();
        content.setTitle(UserInputNormalizer.normalizeName(request.title()));
        content.setDescription(normalizeOptionalText(request.description()));

        return homePageMapper.toContentDto(contentRepository.saveAndFlush(content));
    }

    private String normalizeOptionalText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        return UserInputNormalizer.normalizeName(value);
    }

    private HomePageContent findContentOrThrow() {
        return contentRepository.findById(SINGLETON_CONTENT_ID)
                .orElseThrow(() -> new RequiredSingletonUnavailableException(CONTENT_NOT_FOUND_MESSAGE));
    }

    private HomePageContent findContentForUpdateOrThrow() {
        return contentRepository.findByIdForUpdate(SINGLETON_CONTENT_ID)
                .orElseThrow(() -> new RequiredSingletonUnavailableException(CONTENT_NOT_FOUND_MESSAGE));
    }
}
