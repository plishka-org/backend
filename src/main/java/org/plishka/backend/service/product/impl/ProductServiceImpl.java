package org.plishka.backend.service.product.impl;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.plishka.backend.domain.media.MediaTargetType;
import org.plishka.backend.domain.product.Product;
import org.plishka.backend.domain.product.ProductMedia;
import org.plishka.backend.dto.common.PageResponse;
import org.plishka.backend.dto.file.AttachMediaRequestDto;
import org.plishka.backend.dto.product.ProductDetailDto;
import org.plishka.backend.dto.product.ProductSummaryDto;
import org.plishka.backend.exception.ResourceNotFoundException;
import org.plishka.backend.mapper.product.ProductMapper;
import org.plishka.backend.repository.product.ProductRepository;
import org.plishka.backend.service.file.MediaAttachmentService;
import org.plishka.backend.service.product.ProductMediaQueryService;
import org.plishka.backend.service.product.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {
    private static final Sort RELATED_PRODUCTS_SORT = Sort.by(
            Sort.Order.desc("createdAt"),
            Sort.Order.desc("id")
    );

    private final ProductRepository productRepository;
    private final MediaAttachmentService mediaAttachmentService;
    private final ProductMediaQueryService productMediaQueryService;
    private final ProductMapper productMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryDto> getProducts(
            List<Long> categoryIds,
            String requestedSort,
            int page,
            int size
    ) {
        log.debug(
                "Fetching products: page={}, size={}, sort={}, categoryIds={}",
                page,
                size,
                requestedSort,
                categoryIds
        );

        ProductListCriteria criteria = ProductListCriteria.from(categoryIds, requestedSort);
        Page<Product> productsPage = findProductsPage(criteria, page, size);
        List<ProductSummaryDto> productSummaries = toProductSummariesWithPrimaryMedia(productsPage.getContent());

        log.debug("Successfully fetched {} products", productsPage.getNumberOfElements());

        return PageResponse.from(productsPage, productSummaries);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetailDto getProduct(Long id) {
        log.debug("Fetching product with ID: {}", id);

        Product product = findProductDetailsOrThrow(id);

        log.debug("Successfully fetched product with ID: {}", id);

        return productMapper.toDetailDto(product, product.getMedia());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryDto> getRelatedProducts(Long id, int page, int size) {
        log.debug("Fetching related products for product ID: {}, page={}, size={}", id, page, size);

        Long categoryId = findSourceProductCategoryIdOrThrow(id);
        Page<Product> relatedProductsPage = findRelatedProducts(categoryId, id, page, size);
        List<ProductSummaryDto> relatedProductSummaries = toProductSummariesWithPrimaryMedia(
                relatedProductsPage.getContent()
        );

        log.debug(
                "Successfully fetched {} related products for product ID: {}",
                relatedProductsPage.getNumberOfElements(),
                id
        );

        return PageResponse.from(relatedProductsPage, relatedProductSummaries);
    }

    @Override
    public void attachMedia(Long productId, AttachMediaRequestDto request) {
        mediaAttachmentService.attachMedia(MediaTargetType.PRODUCT, productId, request.s3Key());
    }

    private Page<Product> findProductsPage(ProductListCriteria criteria, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, criteria.sort());

        if (!criteria.hasCategoryFilter()) {
            return productRepository.findAllWithCategory(pageRequest);
        }

        return productRepository.findAllByCategory_IdIn(criteria.categoryIds(), pageRequest);
    }

    private Page<Product> findRelatedProducts(Long categoryId, Long sourceProductId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, RELATED_PRODUCTS_SORT);

        return productRepository.findAllByCategory_IdAndIdNot(
                categoryId,
                sourceProductId,
                pageRequest
        );
    }

    private List<ProductSummaryDto> toProductSummariesWithPrimaryMedia(List<Product> products) {
        Map<Long, ProductMedia> primaryMediaByProductId = productMediaQueryService.findPrimaryMediaForProducts(
                products
        );

        return products.stream()
                .map(product -> productMapper.toSummaryDto(product, primaryMediaByProductId.get(product.getId())))
                .toList();
    }

    private Long findSourceProductCategoryIdOrThrow(Long productId) {
        return productRepository.findCategoryIdByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID " + productId + " not found"));
    }

    private Product findProductDetailsOrThrow(Long id) {
        return productRepository.findDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID " + id + " not found"));
    }
}
