package org.plishka.backend.service.favorite;

import org.plishka.backend.dto.common.PageResponse;
import org.plishka.backend.dto.favorite.FavoriteAddResult;
import org.plishka.backend.dto.favorite.FavoriteDto;

public interface FavoriteService {
    PageResponse<FavoriteDto> getFavorites(Long userId, int page, int size);

    FavoriteAddResult addFavorite(Long userId, Long productId);

    void deleteFavorite(Long userId, Long productId);
}
