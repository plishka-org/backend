package org.plishka.backend.service.admin.catalog.product;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.domain.product.Category;
import org.plishka.backend.domain.product.Product;
import org.plishka.backend.domain.product.ProductMedia;
import org.plishka.backend.dto.admin.common.BulkOperationResultDto;
import org.plishka.backend.dto.admin.product.AdminProductDetailDto;
import org.plishka.backend.dto.admin.product.AdminProductListRequestDto;
import org.plishka.backend.dto.admin.product.AdminProductRequestDto;
import org.plishka.backend.dto.admin.product.BulkProductCategoryRequestDto;
import org.plishka.backend.dto.admin.product.BulkProductDeleteRequestDto;
import org.plishka.backend.dto.admin.product.BulkProductPriceRequestDto;
import org.plishka.backend.dto.common.PageResponse;
import org.plishka.backend.exception.BadRequestException;
import org.plishka.backend.exception.ConflictException;
import org.plishka.backend.exception.ResourceNotFoundException;
import org.plishka.backend.repository.product.CategoryRepository;
import org.plishka.backend.repository.product.ProductRepository;
import org.plishka.backend.service.admin.catalog.support.AdminProductBulkTargetResolver;
import org.plishka.backend.service.admin.catalog.support.AdminProductSpecifications;
import org.plishka.backend.service.admin.catalog.support.ProductPriceCalculator;
import org.plishka.backend.service.product.ProductMediaQueryService;
import org.plishka.backend.service.product.ProductSortResolver;
import org.plishka.backend.util.UserInputNormalizer;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminProductServiceImpl implements AdminProductService {
    private static final String PRODUCT_NAME_UNIQUE_CONSTRAINT = "uk_products_name";
    private static final String PRODUCT_ALREADY_EXISTS_MESSAGE = "Product with this name already exists";
    private static final String PRODUCT_NOT_FOUND_MESSAGE = "Product with ID %d not found";
    private static final String PRODUCT_CATEGORY_REQUIRED_MESSAGE = "Product category is required";
    private static final String TARGET_CATEGORY_REQUIRED_MESSAGE = "Target category is required";

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductDeletionService productDeletionService;
    private final AdminProductBulkTargetResolver productBulkTargetResolver;
    private final ProductMediaQueryService productMediaQueryService;
    private final AdminProductDtoAssembler productDtoAssembler;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminProductDetailDto> getProducts(AdminProductListRequestDto request, int page, int size) {
        Specification<Product> productFilters = AdminProductSpecifications.fromFilters(request.filters());
        PageRequest pageRequest = PageRequest.of(page, size, ProductSortResolver.resolve(request.sort()));
        Page<Product> productsPage = productRepository.findAll(productFilters, pageRequest);
        Map<Long, List<ProductMedia>> mediaByProductId = productMediaQueryService.findMediaForProducts(
                productsPage.getContent()
        );
        List<AdminProductDetailDto> products = productDtoAssembler.toDtos(productsPage.getContent(), mediaByProductId);

        return PageResponse.from(productsPage, products);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminProductDetailDto getProduct(Long productId) {
        Product product = productRepository.findDetailsById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(PRODUCT_NOT_FOUND_MESSAGE.formatted(productId)));

        return productDtoAssembler.toDto(product);
    }

    @Override
    @Transactional
    public AdminProductDetailDto createProduct(AdminProductRequestDto request) {
        String productName = UserInputNormalizer.normalizeName(request.name());
        requireProductNameAvailable(productName);
        Long categoryId = requireCategoryId(request.categoryId(), PRODUCT_CATEGORY_REQUIRED_MESSAGE);
        Category category = findCategoryForAssignment(categoryId);

        Product product = buildProduct(productName, request, category);
        Product savedProduct = saveProductOrThrowConflict(product);

        return productDtoAssembler.toDto(savedProduct, List.of());
    }

    @Override
    @Transactional
    public AdminProductDetailDto updateProduct(Long productId, AdminProductRequestDto request) {
        Long categoryId = requireCategoryId(request.categoryId(), PRODUCT_CATEGORY_REQUIRED_MESSAGE);
        Category targetCategory = findCategoryForAssignment(categoryId);
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new ResourceNotFoundException(PRODUCT_NOT_FOUND_MESSAGE.formatted(productId)));

        String productName = UserInputNormalizer.normalizeName(request.name());
        requireProductNameAvailableForUpdate(productName, productId);
        applyProductState(product, productName, request);
        product.setCategory(targetCategory);

        return productDtoAssembler.toDto(saveProductOrThrowConflict(product));
    }

    @Override
    @Transactional
    public void deleteProduct(Long productId) {
        productDeletionService.deleteProductsByIdsOrThrowIfMissing(List.of(productId));
    }

    @Override
    @Transactional
    public BulkOperationResultDto deleteProducts(BulkProductDeleteRequestDto request) {
        List<Long> targetIds = productBulkTargetResolver.resolveTargetIds(
                request.selectionMode(),
                request.productIds(),
                request.filters()
        );

        return new BulkOperationResultDto(productDeletionService.deleteProductsByIdsOrThrowIfMissing(targetIds));
    }

    @Override
    @Transactional
    public BulkOperationResultDto updatePrices(BulkProductPriceRequestDto request) {
        List<Product> targetProducts = productBulkTargetResolver.findTargetProductsForUpdate(
                request.selectionMode(),
                request.productIds(),
                request.filters()
        );

        List<ProductPriceUpdate> priceUpdates = targetProducts.stream()
                .map(product -> new ProductPriceUpdate(
                        product,
                        ProductPriceCalculator.calculate(product.getPrice(), request.operation(), request.value())
                ))
                .toList();

        priceUpdates.forEach(priceUpdate -> priceUpdate.product().setPrice(priceUpdate.price()));

        return new BulkOperationResultDto(targetProducts.size());
    }

    @Override
    @Transactional
    public BulkOperationResultDto updateCategories(BulkProductCategoryRequestDto request) {
        Long categoryId = requireCategoryId(request.categoryId(), TARGET_CATEGORY_REQUIRED_MESSAGE);
        Category targetCategory = findCategoryForAssignment(categoryId);
        List<Product> targetProducts = productBulkTargetResolver.findTargetProductsForUpdate(
                request.selectionMode(),
                request.productIds(),
                request.filters()
        );

        targetProducts.forEach(product -> product.setCategory(targetCategory));

        return new BulkOperationResultDto(targetProducts.size());
    }

    private Product saveProductOrThrowConflict(Product product) {
        try {
            return productRepository.saveAndFlush(product);
        } catch (DataIntegrityViolationException exception) {
            if (isProductNameConstraintViolation(exception)) {
                throw new ConflictException(PRODUCT_ALREADY_EXISTS_MESSAGE);
            }

            throw exception;
        }
    }

    private Product buildProduct(String productName, AdminProductRequestDto request, Category category) {
        Product product = new Product();
        applyProductState(product, productName, request);
        product.setCategory(category);

        return product;
    }

    private void applyProductState(
            Product product,
            String productName,
            AdminProductRequestDto request
    ) {
        product.setName(productName);
        product.setDescription(UserInputNormalizer.normalizeName(request.description()));
        product.setPrice(ProductPriceCalculator.normalize(request.price()));
    }

    private Category findCategoryForAssignment(Long categoryId) {
        return categoryRepository.findByIdForUpdate(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category with ID " + categoryId + " not found"));
    }

    private Long requireCategoryId(Long categoryId, String message) {
        if (categoryId == null) {
            throw new BadRequestException(message);
        }

        return categoryId;
    }

    private void requireProductNameAvailable(String name) {
        if (productRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException(PRODUCT_ALREADY_EXISTS_MESSAGE);
        }
    }

    private void requireProductNameAvailableForUpdate(String name, Long productId) {
        if (productRepository.existsByNameIgnoreCaseAndIdNot(name, productId)) {
            throw new ConflictException(PRODUCT_ALREADY_EXISTS_MESSAGE);
        }
    }

    private boolean isProductNameConstraintViolation(DataIntegrityViolationException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof org.hibernate.exception.ConstraintViolationException violation
                    && PRODUCT_NAME_UNIQUE_CONSTRAINT.equalsIgnoreCase(violation.getConstraintName())) {
                return true;
            }

            String message = cause.getMessage();
            if (message != null && message.contains(PRODUCT_NAME_UNIQUE_CONSTRAINT)) {
                return true;
            }

            cause = cause.getCause();
        }

        return false;
    }

    private record ProductPriceUpdate(Product product, BigDecimal price) {
    }
}
