package org.plishka.backend.service.favorite.impl;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.plishka.backend.domain.favorite.Favorite;
import org.plishka.backend.domain.product.Product;
import org.plishka.backend.domain.user.User;
import org.plishka.backend.dto.common.PageResponse;
import org.plishka.backend.dto.favorite.FavoriteAddResult;
import org.plishka.backend.dto.favorite.FavoriteDto;
import org.plishka.backend.exception.ResourceNotFoundException;
import org.plishka.backend.mapper.favorite.FavoriteMapper;
import org.plishka.backend.repository.favorite.FavoriteRepository;
import org.plishka.backend.repository.product.ProductRepository;
import org.plishka.backend.service.favorite.FavoriteService;
import org.plishka.backend.service.product.ProductSummaryAssembler;
import org.plishka.backend.service.user.EligibleUserProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FavoriteServiceImpl implements FavoriteService {
    private static final String FAVORITES_USER_PRODUCT_CONSTRAINT = "uk_favorites_user_product";
    private static final Sort FAVORITES_SORT = Sort.by(
            Sort.Order.desc("createdAt"),
            Sort.Order.desc("id")
    );

    private final FavoriteRepository favoriteRepository;
    private final ProductRepository productRepository;
    private final EligibleUserProvider eligibleUserProvider;
    private final ProductSummaryAssembler productSummaryAssembler;
    private final FavoriteMapper favoriteMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FavoriteDto> getFavorites(Long userId, int page, int size) {
        Page<Favorite> favoritesPage = favoriteRepository.findAllByUser_Id(
                userId,
                PageRequest.of(page, size, FAVORITES_SORT)
        );
        List<FavoriteDto> content = toFavoriteDtos(favoritesPage.getContent());

        return PageResponse.from(favoritesPage, content);
    }

    @Override
    @Transactional
    public FavoriteAddResult addFavorite(Long userId, Long productId) {
        User user = eligibleUserProvider.getEligibleUserOrThrow(userId);
        Product product = findProductByIdOrThrow(productId);

        return favoriteRepository.findByUser_IdAndProduct_Id(userId, productId)
                .map(favorite -> new FavoriteAddResult(toFavoriteDto(favorite), false))
                .orElseGet(() -> createFavorite(user, product));
    }

    @Override
    @Transactional
    public void deleteFavorite(Long userId, Long productId) {
        eligibleUserProvider.getEligibleUserOrThrow(userId);

        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product with ID " + productId + " not found");
        }

        favoriteRepository.deleteByUserIdAndProductId(userId, productId);

        log.info("Favorite delete requested: userId={}, productId={}", userId, productId);
    }

    private FavoriteAddResult createFavorite(User user, Product product) {
        Favorite favorite = Favorite.builder()
                .user(user)
                .product(product)
                .build();

        try {
            Favorite savedFavorite = favoriteRepository.saveAndFlush(favorite);
            log.info("Favorite created: userId={}, productId={}", user.getId(), product.getId());
            return new FavoriteAddResult(toFavoriteDto(savedFavorite), true);
        } catch (DataIntegrityViolationException exception) {
            if (!isFavoriteUniqueConstraintViolation(exception)) {
                throw exception;
            }

            return favoriteRepository.findByUser_IdAndProduct_Id(user.getId(), product.getId())
                    .map(existingFavorite -> new FavoriteAddResult(toFavoriteDto(existingFavorite), false))
                    .orElseThrow(() -> exception);
        }
    }

    private List<FavoriteDto> toFavoriteDtos(List<Favorite> favorites) {
        var productSummariesById = productSummaryAssembler.toDtoMapForProducts(
                favorites.stream()
                        .map(Favorite::getProduct)
                        .toList()
        );

        return favorites.stream()
                .map(favorite -> favoriteMapper.toDto(
                        favorite,
                        productSummariesById.get(favorite.getProduct().getId())
                ))
                .toList();
    }

    private FavoriteDto toFavoriteDto(Favorite favorite) {
        return favoriteMapper.toDto(
                favorite,
                productSummaryAssembler.toDto(favorite.getProduct())
        );
    }

    private Product findProductByIdOrThrow(Long productId) {
        return productRepository.findByIdWithCategory(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID " + productId + " not found"));
    }

    private boolean isFavoriteUniqueConstraintViolation(DataIntegrityViolationException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof org.hibernate.exception.ConstraintViolationException violation
                    && FAVORITES_USER_PRODUCT_CONSTRAINT.equalsIgnoreCase(violation.getConstraintName())) {
                return true;
            }

            String message = cause.getMessage();
            if (message != null && message.contains(FAVORITES_USER_PRODUCT_CONSTRAINT)) {
                return true;
            }

            cause = cause.getCause();
        }

        return false;
    }
}
