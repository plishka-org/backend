package org.plishka.backend.dto.contacts;

import java.util.List;

public record ContactsPageResponse(
        String phoneNumber,
        String email,
        String address,
        String googleMapsUrl,
        List<SocialLinkDto> socialLinks
) {
}
