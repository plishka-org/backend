package org.plishka.backend.dto.admin.contacts;

import java.util.List;
import org.plishka.backend.dto.contacts.ContactsPageContentDto;

public record AdminContactsPageDto(
        ContactsPageContentDto content,
        List<AdminContactsPageSocialLinkDto> socialLinks
) {
}
