package org.plishka.backend.dto.favorite;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Internal favorite add result.")
public record FavoriteAddResult(
        @Schema(description = "Favorite data.")
        FavoriteDto favorite,
        @Schema(description = "Whether a new favorite was created.", example = "true")
        boolean created
) {
}
