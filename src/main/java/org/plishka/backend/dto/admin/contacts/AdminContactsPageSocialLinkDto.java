package org.plishka.backend.dto.admin.contacts;

public record AdminContactsPageSocialLinkDto(
        Long socialLinkId,
        String name,
        String url,
        Integer displayOrder
) {
}
