package org.plishka.backend.service.contacts.impl;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.plishka.backend.exception.RequiredSingletonUnavailableException;
import org.plishka.backend.mapper.contacts.ContactsPageMapper;
import org.plishka.backend.repository.contacts.ContactsPageRepository;
import org.plishka.backend.repository.contacts.SocialLinkRepository;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContactsPageServiceImplTest {
    private static final long CONTENT_ID = 1L;

    @Mock
    private ContactsPageRepository contactsPageRepository;

    @Mock
    private SocialLinkRepository socialLinkRepository;

    @Mock
    private ContactsPageMapper contactsPageMapper;

    @InjectMocks
    private ContactsPageServiceImpl service;

    @Test
    void getContactsPageData_ShouldThrowOperationalException_WhenContentMissing() {
        when(contactsPageRepository.findById(CONTENT_ID)).thenReturn(Optional.empty());

        assertThrows(RequiredSingletonUnavailableException.class, service::getContactsPageData);
    }
}
