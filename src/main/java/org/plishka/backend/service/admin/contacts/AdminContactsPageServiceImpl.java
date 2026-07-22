package org.plishka.backend.service.admin.contacts;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.domain.contacts.ContactsPage;
import org.plishka.backend.domain.contacts.SocialLink;
import org.plishka.backend.dto.admin.contacts.AdminContactsPageContentRequestDto;
import org.plishka.backend.dto.admin.contacts.AdminContactsPageDto;
import org.plishka.backend.dto.admin.contacts.AdminContactsPageSocialLinkDto;
import org.plishka.backend.dto.admin.contacts.AdminContactsPageSocialLinkRequestDto;
import org.plishka.backend.dto.contacts.ContactsPageContentDto;
import org.plishka.backend.exception.BadRequestException;
import org.plishka.backend.exception.RequiredSingletonUnavailableException;
import org.plishka.backend.exception.ResourceNotFoundException;
import org.plishka.backend.mapper.contacts.ContactsPageMapper;
import org.plishka.backend.repository.contacts.ContactsPageRepository;
import org.plishka.backend.repository.contacts.SocialLinkRepository;
import org.plishka.backend.util.UserInputNormalizer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AdminContactsPageServiceImpl implements AdminContactsPageService {
    private static final long SINGLETON_CONTENT_ID = 1L;
    private static final int MAX_SOCIAL_LINKS = 20;
    private static final String CONTENT_NOT_FOUND_MESSAGE = "Contacts page content not found";
    private static final String SOCIAL_LINK_NOT_FOUND_MESSAGE = "Contacts page social link with ID %d not found";
    private static final String SOCIAL_LINK_LIMIT_MESSAGE = "Contacts page cannot contain more than %d social links";

    private final ContactsPageRepository contactsPageRepository;
    private final SocialLinkRepository socialLinkRepository;
    private final ContactsPageMapper contactsPageMapper;

    @Override
    @Transactional(readOnly = true)
    public AdminContactsPageDto getContactsPage() {
        ContactsPage contactsPage = findContactsPageOrThrow();
        List<AdminContactsPageSocialLinkDto> socialLinks =
                socialLinkRepository.findAllByContactsPageIdOrderByUpdatedAtDescIdDesc(SINGLETON_CONTENT_ID)
                        .stream()
                        .map(contactsPageMapper::toAdminSocialLinkDto)
                        .toList();

        return new AdminContactsPageDto(contactsPageMapper.toContentDto(contactsPage), socialLinks);
    }

    @Override
    @Transactional
    public ContactsPageContentDto updateContactsPageContent(AdminContactsPageContentRequestDto request) {
        ContactsPage contactsPage = findContactsPageForUpdateOrThrow();
        contactsPage.setPhoneNumber(UserInputNormalizer.normalizePhone(request.phoneNumber()));
        contactsPage.setEmail(normalizeOptionalEmail(request.email()));
        contactsPage.setAddress(normalizeOptionalText(request.address()));
        contactsPage.setGoogleMapsUrl(normalizeOptionalUrl(request.googleMapsUrl()));

        return contactsPageMapper.toContentDto(contactsPageRepository.saveAndFlush(contactsPage));
    }

    @Override
    @Transactional
    public AdminContactsPageSocialLinkDto createSocialLink(AdminContactsPageSocialLinkRequestDto request) {
        findContactsPageForUpdateOrThrow();
        requireSocialLinkCapacityAvailable();

        SocialLink socialLink = new SocialLink();
        applySocialLinkState(socialLink, request);
        socialLink.setContactsPageId(SINGLETON_CONTENT_ID);

        return contactsPageMapper.toAdminSocialLinkDto(socialLinkRepository.saveAndFlush(socialLink));
    }

    @Override
    @Transactional
    public AdminContactsPageSocialLinkDto updateSocialLink(
            Long socialLinkId,
            AdminContactsPageSocialLinkRequestDto request
    ) {
        SocialLink socialLink = findSocialLinkForUpdateOrThrow(socialLinkId);
        applySocialLinkState(socialLink, request);

        return contactsPageMapper.toAdminSocialLinkDto(socialLinkRepository.saveAndFlush(socialLink));
    }

    @Override
    @Transactional
    public void deleteSocialLink(Long socialLinkId) {
        SocialLink socialLink = findSocialLinkForUpdateOrThrow(socialLinkId);

        socialLinkRepository.delete(socialLink);
        socialLinkRepository.flush();
    }

    private void requireSocialLinkCapacityAvailable() {
        List<SocialLink> socialLinks =
                socialLinkRepository.findAllByContactsPageIdForUpdateOrderByUpdatedAtDescIdDesc(SINGLETON_CONTENT_ID);
        if (socialLinks.size() >= MAX_SOCIAL_LINKS) {
            throw new BadRequestException(SOCIAL_LINK_LIMIT_MESSAGE.formatted(MAX_SOCIAL_LINKS));
        }
    }

    private void applySocialLinkState(SocialLink socialLink, AdminContactsPageSocialLinkRequestDto request) {
        socialLink.setName(UserInputNormalizer.normalizeName(request.name()));
        socialLink.setUrl(request.url().trim());
    }

    private String normalizeOptionalEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return null;
        }

        return UserInputNormalizer.normalizeEmail(email);
    }

    private String normalizeOptionalText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        return UserInputNormalizer.normalizeName(value);
    }

    private String normalizeOptionalUrl(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        return value.trim();
    }

    private ContactsPage findContactsPageOrThrow() {
        return contactsPageRepository.findById(SINGLETON_CONTENT_ID)
                .orElseThrow(() -> new RequiredSingletonUnavailableException(CONTENT_NOT_FOUND_MESSAGE));
    }

    private ContactsPage findContactsPageForUpdateOrThrow() {
        return contactsPageRepository.findByIdForUpdate(SINGLETON_CONTENT_ID)
                .orElseThrow(() -> new RequiredSingletonUnavailableException(CONTENT_NOT_FOUND_MESSAGE));
    }

    private SocialLink findSocialLinkForUpdateOrThrow(Long socialLinkId) {
        return socialLinkRepository.findByContactsPageIdAndIdForUpdate(SINGLETON_CONTENT_ID, socialLinkId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        SOCIAL_LINK_NOT_FOUND_MESSAGE.formatted(socialLinkId)
                ));
    }
}
