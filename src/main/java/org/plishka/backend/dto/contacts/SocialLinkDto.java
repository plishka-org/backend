package org.plishka.backend.dto.contacts;

public record SocialLinkDto(
        Long socialLinkId,
        String name,
        String url
) {
}
