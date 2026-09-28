package org.plishka.backend.service.admin.contacts;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminContactsPageServiceImplTest {
    private static final long CONTENT_ID = 1L;
    private static final String GOOGLE_MAPS_URL = "https://maps.google.com/?q=Kyiv";

    @Mock
    private ContactsPageRepository contactsPageRepository;

    @Mock
    private SocialLinkRepository socialLinkRepository;

    @Mock
    private ContactsPageMapper contactsPageMapper;

    @InjectMocks
    private AdminContactsPageServiceImpl service;

    @Test
    void getContactsPage_ShouldReturnMappedContactsPage() {
        ContactsPage contactsPage = contactsPage("mail@test.com", "Address", GOOGLE_MAPS_URL);
        SocialLink socialLink = socialLink(5L, "Instagram", "https://instagram.com/plishka");
        ContactsPageContentDto contentDto = new ContactsPageContentDto("+380", "mail@test.com", "Address", GOOGLE_MAPS_URL);
        AdminContactsPageSocialLinkDto socialLinkDto =
                new AdminContactsPageSocialLinkDto(5L, "Instagram", "https://instagram.com/plishka");

        when(contactsPageRepository.findById(CONTENT_ID)).thenReturn(Optional.of(contactsPage));
        when(socialLinkRepository.findAllByContactsPageIdOrderByUpdatedAtDescIdDesc(CONTENT_ID))
                .thenReturn(List.of(socialLink));
        when(contactsPageMapper.toContentDto(contactsPage)).thenReturn(contentDto);
        when(contactsPageMapper.toAdminSocialLinkDto(socialLink)).thenReturn(socialLinkDto);

        AdminContactsPageDto result = service.getContactsPage();

        assertEquals(contentDto, result.content());
        assertEquals(List.of(socialLinkDto), result.socialLinks());
    }

    @Test
    void updateContactsPageContent_ShouldPersistNormalizedContent() {
        ContactsPage contactsPage = contactsPage("OLD@MAIL.COM", "Old address", " https://maps.google.com/?q=old");
        givenContactsPageLocked(contactsPage);
        when(contactsPageRepository.saveAndFlush(contactsPage)).thenReturn(contactsPage);
        when(contactsPageMapper.toContentDto(contactsPage)).thenReturn(
                new ContactsPageContentDto("+380501234567", "new@mail.com", "New address", GOOGLE_MAPS_URL)
        );

        service.updateContactsPageContent(new AdminContactsPageContentRequestDto(
                "+380501234567",
                "NEW@MAIL.COM",
                "New address",
                GOOGLE_MAPS_URL
        ));

        assertEquals("+380501234567", contactsPage.getPhoneNumber());
        assertEquals("new@mail.com", contactsPage.getEmail());
        assertEquals("New address", contactsPage.getAddress());
        assertEquals(GOOGLE_MAPS_URL, contactsPage.getGoogleMapsUrl());
        verify(contactsPageMapper).toContentDto(contactsPage);
    }

    @Test
    void updateContactsPageContent_ShouldNormalizeBlankOptionalFieldsToNull() {
        ContactsPage contactsPage = contactsPage("mail@test.com", "Address", GOOGLE_MAPS_URL);
        givenContactsPageLocked(contactsPage);
        when(contactsPageRepository.saveAndFlush(contactsPage)).thenReturn(contactsPage);

        service.updateContactsPageContent(new AdminContactsPageContentRequestDto("", "", "", ""));

        assertNull(contactsPage.getPhoneNumber());
        assertNull(contactsPage.getEmail());
        assertNull(contactsPage.getAddress());
        assertNull(contactsPage.getGoogleMapsUrl());
    }

    @Test
    void createSocialLink_ShouldPersistNormalizedState() {
        givenContactsPageLocked(contactsPage("mail@test.com", "Address", GOOGLE_MAPS_URL));
        givenExistingSocialLinks(
                socialLink(1L, "Facebook", "https://facebook.com/plishka"),
                socialLink(2L, "Twitter", "https://twitter.com/plishka")
        );
        when(socialLinkRepository.saveAndFlush(any(SocialLink.class))).thenAnswer(invocation -> {
            SocialLink socialLink = invocation.getArgument(0);
            socialLink.setId(10L);
            return socialLink;
        });
        when(contactsPageMapper.toAdminSocialLinkDto(any(SocialLink.class))).thenAnswer(invocation -> {
            SocialLink savedSocialLink = invocation.getArgument(0);
            return new AdminContactsPageSocialLinkDto(
                    savedSocialLink.getId(),
                    savedSocialLink.getName(),
                    savedSocialLink.getUrl()
            );
        });

        service.createSocialLink(new AdminContactsPageSocialLinkRequestDto("Instagram", " https://instagram.com/plishka "));

        ArgumentCaptor<SocialLink> socialLinkCaptor = ArgumentCaptor.forClass(SocialLink.class);
        verify(socialLinkRepository).saveAndFlush(socialLinkCaptor.capture());
        SocialLink savedSocialLink = socialLinkCaptor.getValue();
        assertEquals("Instagram", savedSocialLink.getName());
        assertEquals("https://instagram.com/plishka", savedSocialLink.getUrl());
        assertEquals(CONTENT_ID, savedSocialLink.getContactsPageId());
    }

    @Test
    void createSocialLink_ShouldRejectWhenLimitReached() {
        givenContactsPageLocked(contactsPage("mail@test.com", "Address", GOOGLE_MAPS_URL));
        givenSocialLinksAtCapacity();

        assertThrows(
                BadRequestException.class,
                () -> service.createSocialLink(new AdminContactsPageSocialLinkRequestDto("Instagram", "https://instagram.com/plishka"))
        );

        verify(socialLinkRepository, never()).saveAndFlush(any());
    }

    @Test
    void createSocialLink_ShouldThrow_WhenContactsPageMissing() {
        when(contactsPageRepository.findByIdForUpdate(CONTENT_ID)).thenReturn(Optional.empty());

        assertThrows(
                RequiredSingletonUnavailableException.class,
                () -> service.createSocialLink(
                        new AdminContactsPageSocialLinkRequestDto("Instagram", "https://instagram.com/plishka")
                )
        );

        verify(socialLinkRepository, never()).saveAndFlush(any());
    }

    @Test
    void getContactsPage_ShouldThrowOperationalException_WhenContentMissing() {
        when(contactsPageRepository.findById(CONTENT_ID)).thenReturn(Optional.empty());

        assertThrows(RequiredSingletonUnavailableException.class, service::getContactsPage);
    }

    @Test
    void updateContactsPageContent_ShouldThrowOperationalException_WhenContentMissing() {
        when(contactsPageRepository.findByIdForUpdate(CONTENT_ID)).thenReturn(Optional.empty());

        assertThrows(
                RequiredSingletonUnavailableException.class,
                () -> service.updateContactsPageContent(new AdminContactsPageContentRequestDto(
                        "+380501234567",
                        "mail@test.com",
                        "Address",
                        GOOGLE_MAPS_URL
                ))
        );
    }

    @Test
    void updateSocialLink_ShouldPersistNormalizedState() {
        SocialLink socialLink = socialLink(5L, "Old name", "https://instagram.com/old");
        givenSocialLinkLocked(5L, socialLink);
        when(socialLinkRepository.saveAndFlush(socialLink)).thenReturn(socialLink);
        when(contactsPageMapper.toAdminSocialLinkDto(socialLink)).thenReturn(
                new AdminContactsPageSocialLinkDto(5L, "Telegram", "https://t.me/plishka")
        );

        service.updateSocialLink(5L, new AdminContactsPageSocialLinkRequestDto("Telegram", " https://t.me/plishka "));

        assertEquals("Telegram", socialLink.getName());
        assertEquals("https://t.me/plishka", socialLink.getUrl());
    }

    @Test
    void deleteSocialLink_ShouldRemoveSocialLink() {
        SocialLink socialLink = socialLink(7L, "Instagram", "https://instagram.com/plishka");
        givenSocialLinkLocked(7L, socialLink);

        service.deleteSocialLink(7L);

        verify(socialLinkRepository).delete(socialLink);
        verify(socialLinkRepository).flush();
    }

    @Test
    void updateSocialLink_ShouldThrow_WhenSocialLinkMissing() {
        givenSocialLinkMissing();

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.updateSocialLink(
                        99L,
                        new AdminContactsPageSocialLinkRequestDto("Instagram", "https://instagram.com/plishka")
                )
        );
    }

    private void givenExistingSocialLinks(SocialLink... socialLinks) {
        when(socialLinkRepository.findAllByContactsPageIdForUpdateOrderByUpdatedAtDescIdDesc(CONTENT_ID))
                .thenReturn(List.of(socialLinks));
    }

    private void givenSocialLinksAtCapacity() {
        List<SocialLink> socialLinks = IntStream.rangeClosed(1, 20)
                .mapToObj(index -> socialLink(
                        (long) index,
                        "Link " + index,
                        "https://example.com/" + index
                ))
                .toList();
        when(socialLinkRepository.findAllByContactsPageIdForUpdateOrderByUpdatedAtDescIdDesc(CONTENT_ID))
                .thenReturn(socialLinks);
    }

    private void givenContactsPageLocked(ContactsPage contactsPage) {
        when(contactsPageRepository.findByIdForUpdate(CONTENT_ID)).thenReturn(Optional.of(contactsPage));
    }

    private void givenSocialLinkLocked(long socialLinkId, SocialLink socialLink) {
        when(socialLinkRepository.findByContactsPageIdAndIdForUpdate(CONTENT_ID, socialLinkId))
                .thenReturn(Optional.of(socialLink));
    }

    private void givenSocialLinkMissing() {
        when(socialLinkRepository.findByContactsPageIdAndIdForUpdate(CONTENT_ID, 99L))
                .thenReturn(Optional.empty());
    }

    private static ContactsPage contactsPage(
            String email,
            String address,
            String googleMapsUrl
    ) {
        ContactsPage contactsPage = new ContactsPage();
        contactsPage.setId(AdminContactsPageServiceImplTest.CONTENT_ID);
        contactsPage.setPhoneNumber("+380");
        contactsPage.setEmail(email);
        contactsPage.setAddress(address);
        contactsPage.setGoogleMapsUrl(googleMapsUrl);
        return contactsPage;
    }

    private static SocialLink socialLink(Long id, String name, String url) {
        SocialLink socialLink = new SocialLink();
        socialLink.setId(id);
        socialLink.setName(name);
        socialLink.setUrl(url);
        socialLink.setContactsPageId(CONTENT_ID);
        return socialLink;
    }
}
