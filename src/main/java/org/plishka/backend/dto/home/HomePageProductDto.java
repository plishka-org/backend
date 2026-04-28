package org.plishka.backend.dto.home;

public record HomePageProductDto(
        Long productId,
        String name,
        String photoUrl
) {
}
