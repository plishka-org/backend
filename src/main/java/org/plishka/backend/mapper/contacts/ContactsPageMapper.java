package org.plishka.backend.mapper.contacts;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.plishka.backend.config.MapStructConfig;
import org.plishka.backend.domain.contacts.ContactsPage;
import org.plishka.backend.domain.contacts.SocialLink;
import org.plishka.backend.dto.admin.contacts.AdminContactsPageSocialLinkDto;
import org.plishka.backend.dto.contacts.ContactsPageContentDto;
import org.plishka.backend.dto.contacts.ContactsPageResponse;
import org.plishka.backend.dto.contacts.SocialLinkDto;

@Mapper(config = MapStructConfig.class)
public interface ContactsPageMapper {
    @Mapping(target = "socialLinks", source = "socialLinks")
    ContactsPageResponse toResponse(ContactsPage contactsPage, List<SocialLink> socialLinks);

    ContactsPageContentDto toContentDto(ContactsPage contactsPage);

    @Mapping(target = "socialLinkId", source = "id")
    SocialLinkDto toSocialLinkDto(SocialLink socialLink);

    @Mapping(target = "socialLinkId", source = "id")
    AdminContactsPageSocialLinkDto toAdminSocialLinkDto(SocialLink socialLink);
}
