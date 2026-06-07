package org.plishka.backend.dto.favorite;

public record FavoriteAddResult(
        FavoriteDto favorite,
        boolean created
) {
}
