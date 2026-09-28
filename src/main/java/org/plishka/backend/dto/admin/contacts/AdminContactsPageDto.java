package org.plishka.backend.dto.admin.contacts;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.plishka.backend.dto.contacts.ContactsPageContentDto;

@Schema(description = "Admin contacts page response.")
public record AdminContactsPageDto(
        @Schema(description = "Contacts page content.")
        ContactsPageContentDto content,
        @Schema(description = "Social links.")
        List<AdminContactsPageSocialLinkDto> socialLinks
) {
}
