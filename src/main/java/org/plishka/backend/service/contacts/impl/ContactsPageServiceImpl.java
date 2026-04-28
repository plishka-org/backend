package org.plishka.backend.service.contacts.impl;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.plishka.backend.domain.contacts.ContactsPage;
import org.plishka.backend.domain.contacts.SocialLink;
import org.plishka.backend.dto.contacts.ContactsPageResponse;
import org.plishka.backend.dto.contacts.SocialLinkDto;
import org.plishka.backend.repository.contacts.ContactsPageRepository;
import org.plishka.backend.repository.contacts.SocialLinkRepository;
import org.plishka.backend.service.contacts.ContactsPageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContactsPageServiceImpl implements ContactsPageService {
    private static final long SINGLETON_CONTENT_ID = 1L;

    private final ContactsPageRepository contactsPageRepository;
    private final SocialLinkRepository socialLinkRepository;

    @Override
    @Transactional(readOnly = true)
    public ContactsPageResponse getContactsPageData() {
        log.debug("Fetching contacts page data");

        ContactsPage contactsPage = findContactsPageOrThrow();
        List<SocialLinkDto> socialLinks = getSocialLinks();

        ContactsPageResponse response = new ContactsPageResponse(
                contactsPage.getPhoneNumber(),
                contactsPage.getEmail(),
                contactsPage.getAddress(),
                contactsPage.getGoogleMapsUrl(),
                socialLinks
        );

        log.info("Contacts page data fetched successfully: socialLinksCount={}", socialLinks.size());

        return response;
    }

    private ContactsPage findContactsPageOrThrow() {
        return contactsPageRepository.findById(SINGLETON_CONTENT_ID)
                .orElseThrow(() -> new IllegalStateException(
                        "Contacts page content not found. Please verify database initialization."
                ));
    }

    private List<SocialLinkDto> getSocialLinks() {
        return socialLinkRepository.findAllByContactsPageIdOrderByDisplayOrderAsc(SINGLETON_CONTENT_ID)
                .stream()
                .map(this::mapToSocialLinkDto)
                .toList();
    }

    private SocialLinkDto mapToSocialLinkDto(SocialLink link) {
        return new SocialLinkDto(
                link.getId(),
                link.getName(),
                link.getUrl()
        );
    }
}
