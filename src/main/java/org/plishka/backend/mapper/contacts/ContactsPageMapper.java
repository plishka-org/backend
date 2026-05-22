package org.plishka.backend.mapper.contacts;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.plishka.backend.config.MapStructConfig;
import org.plishka.backend.domain.contacts.ContactsPage;
import org.plishka.backend.domain.contacts.SocialLink;
import org.plishka.backend.dto.contacts.ContactsPageResponse;
import org.plishka.backend.dto.contacts.SocialLinkDto;

@Mapper(config = MapStructConfig.class)
public interface ContactsPageMapper {
    @Mapping(target = "socialLinks", source = "socialLinks")
    ContactsPageResponse toResponse(ContactsPage contactsPage, List<SocialLink> socialLinks);

    @Mapping(target = "socialLinkId", source = "id")
    SocialLinkDto toSocialLinkDto(SocialLink socialLink);
}
