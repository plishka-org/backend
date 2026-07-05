package org.plishka.backend.dto.contacts;

public record ContactsPageContentDto(
        String phoneNumber,
        String email,
        String address,
        String googleMapsUrl
) {
}
