package org.plishka.backend.service.product.impl;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.plishka.backend.domain.product.Product;
import org.plishka.backend.domain.product.ProductView;
import org.plishka.backend.domain.user.User;
import org.plishka.backend.dto.product.ProductViewDto;
import org.plishka.backend.exception.ResourceNotFoundException;
import org.plishka.backend.repository.product.ProductRepository;
import org.plishka.backend.repository.product.ProductViewRepository;
import org.plishka.backend.service.product.ProductViewDtoAssembler;
import org.plishka.backend.service.product.ProductViewService;
import org.plishka.backend.service.user.EligibleUserProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductViewServiceImpl implements ProductViewService {
    private static final int RECENT_PRODUCT_VIEW_LIMIT = 10;
    private static final String PRODUCT_VIEWS_USER_PRODUCT_CONSTRAINT = "uk_product_views_user_product";

    private final ProductViewRepository productViewRepository;
    private final ProductRepository productRepository;
    private final EligibleUserProvider eligibleUserProvider;
    private final ProductViewDtoAssembler productViewDtoAssembler;
    private final Clock clock;

    @Override
    @Transactional
    public ProductViewDto recordProductView(Long userId, Long productId) {
        User viewer = eligibleUserProvider.getEligibleUserOrThrow(userId);
        Product viewedProduct = findProductByIdOrThrow(productId);
        Instant viewedAt = Instant.now(clock);

        ProductView productView = recordOrRefreshProductView(viewer, viewedProduct, viewedAt);
        enforceRecentViewLimit(viewer.getId());

        log.info("Product view recorded: userId={}, productId={}", userId, productId);

        return productViewDtoAssembler.toDto(productView);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductViewDto> getViewedProducts(Long userId) {
        List<ProductView> recentViews = productViewRepository.findAllByUser_IdOrderByViewedAtDescIdDesc(userId);
        return productViewDtoAssembler.toDtos(recentViews);
    }

    private ProductView recordOrRefreshProductView(User viewer, Product viewedProduct, Instant viewedAt) {
        return productViewRepository.findByUser_IdAndProduct_Id(viewer.getId(), viewedProduct.getId())
                .map(existingView -> {
                    existingView.setViewedAt(viewedAt);
                    return existingView;
                })
                .orElseGet(() -> createProductView(viewer, viewedProduct, viewedAt));
    }

    private ProductView createProductView(User viewer, Product viewedProduct, Instant viewedAt) {
        ProductView productView = ProductView.builder()
                .user(viewer)
                .product(viewedProduct)
                .viewedAt(viewedAt)
                .build();

        try {
            return productViewRepository.saveAndFlush(productView);
        } catch (DataIntegrityViolationException exception) {
            if (!isProductViewUniqueConstraintViolation(exception)) {
                throw exception;
            }

            ProductView existingView = productViewRepository.findByUser_IdAndProduct_Id(
                            viewer.getId(),
                            viewedProduct.getId()
                    )
                    .orElseThrow(() -> exception);
            existingView.setViewedAt(viewedAt);
            return existingView;
        }
    }

    private void enforceRecentViewLimit(Long userId) {
        long viewsOverLimit = productViewRepository.countByUser_Id(userId) - RECENT_PRODUCT_VIEW_LIMIT;
        if (viewsOverLimit <= 0) {
            return;
        }

        List<Long> oldestViewIds = productViewRepository.findAllByUser_IdOrderByViewedAtAscIdAsc(
                        userId,
                        PageRequest.of(0, Math.toIntExact(viewsOverLimit))
                )
                .stream()
                .map(ProductView::getId)
                .toList();

        if (!oldestViewIds.isEmpty()) {
            productViewRepository.deleteAllByIdIn(oldestViewIds);
        }
    }

    private Product findProductByIdOrThrow(Long productId) {
        return productRepository.findByIdWithCategory(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID " + productId + " not found"));
    }

    private boolean isProductViewUniqueConstraintViolation(DataIntegrityViolationException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof org.hibernate.exception.ConstraintViolationException violation
                    && PRODUCT_VIEWS_USER_PRODUCT_CONSTRAINT.equalsIgnoreCase(violation.getConstraintName())) {
                return true;
            }

            String message = cause.getMessage();
            if (message != null && message.contains(PRODUCT_VIEWS_USER_PRODUCT_CONSTRAINT)) {
                return true;
            }

            cause = cause.getCause();
        }

        return false;
    }
}
