package org.plishka.backend.controller.favorite;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.dto.common.PageResponse;
import org.plishka.backend.dto.common.PaginationRequestDto;
import org.plishka.backend.dto.favorite.FavoriteAddResult;
import org.plishka.backend.dto.favorite.FavoriteDto;
import org.plishka.backend.security.AuthenticatedUserPrincipal;
import org.plishka.backend.service.favorite.FavoriteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users/me/favorites")
@RequiredArgsConstructor
public class FavoriteController {
    private static final int DEFAULT_PAGE_SIZE = 16;

    private final FavoriteService favoriteService;

    @GetMapping
    public PageResponse<FavoriteDto> getFavorites(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Valid @ModelAttribute PaginationRequestDto paginationRequest
    ) {
        return favoriteService.getFavorites(
                principal.getUserId(),
                paginationRequest.resolvePage(),
                paginationRequest.resolveSize(DEFAULT_PAGE_SIZE)
        );
    }

    @PostMapping("/{productId}")
    public ResponseEntity<FavoriteDto> addFavorite(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Positive @PathVariable Long productId
    ) {
        FavoriteAddResult result = favoriteService.addFavorite(principal.getUserId(), productId);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.favorite());
    }

    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFavorite(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Positive @PathVariable Long productId
    ) {
        favoriteService.deleteFavorite(principal.getUserId(), productId);
    }
}
