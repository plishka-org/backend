package org.plishka.backend.service.admin.catalog.category;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.domain.product.Category;
import org.plishka.backend.dto.admin.category.AdminCategoryRequestDto;
import org.plishka.backend.dto.admin.category.CategoryDeleteStrategy;
import org.plishka.backend.dto.admin.category.CategoryOrderRequestDto;
import org.plishka.backend.dto.product.CategoryDto;
import org.plishka.backend.exception.BadRequestException;
import org.plishka.backend.exception.ConflictException;
import org.plishka.backend.exception.ResourceNotFoundException;
import org.plishka.backend.mapper.product.CategoryMapper;
import org.plishka.backend.repository.home.HomePageProductRepository;
import org.plishka.backend.repository.product.CategoryRepository;
import org.plishka.backend.repository.product.ProductRepository;
import org.plishka.backend.service.admin.catalog.product.ProductDeletionService;
import org.plishka.backend.util.UserInputNormalizer;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AdminCategoryServiceImpl implements AdminCategoryService {
    private static final String CATEGORY_NAME_UNIQUE_CONSTRAINT = "uk_categories_name";
    private static final String CATEGORY_ALREADY_EXISTS_MESSAGE = "Category with this name already exists";
    private static final String CATEGORY_NOT_FOUND_MESSAGE = "Category with ID %d not found";
    private static final String CATEGORY_DELETE_STRATEGY_REQUIRED_MESSAGE = "Category delete strategy is required";
    private static final String UNSUPPORTED_CATEGORY_DELETE_STRATEGY_MESSAGE = "Unsupported category delete strategy";
    private static final String MOVE_TARGET_REQUIRED_MESSAGE = "Target category is required for MOVE_PRODUCTS strategy";
    private static final String MOVE_TARGET_CANNOT_BE_DELETED_MESSAGE = "Target category cannot be deleted";
    private static final String CATEGORY_ORDER_IDS_REQUIRED_MESSAGE = "Category ids are required";
    private static final String CATEGORY_ORDER_IDS_UNIQUE_MESSAGE = "Category ids must be unique";
    private static final String CATEGORY_ORDER_CURRENT_IDS_MESSAGE =
            "Category order must contain the current category ids";

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final HomePageProductRepository homePageProductRepository;
    private final ProductDeletionService productDeletionService;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDto> getCategories(String search) {
        List<Category> categories = StringUtils.hasText(search)
                ? categoryRepository.findAllByNameContainingIgnoreCaseOrderByDisplayOrderAscIdAsc(search.trim())
                : categoryRepository.findAllByOrderByDisplayOrderAscIdAsc();

        return categories
                .stream()
                .map(categoryMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public CategoryDto createCategory(AdminCategoryRequestDto request) {
        String categoryName = UserInputNormalizer.normalizeName(request.name());
        requireCategoryNameAvailable(categoryName);

        Category category = new Category();
        category.setName(categoryName);
        category.setDisplayOrder(findNextDisplayOrder());

        return categoryMapper.toDto(saveCategoryOrThrowConflict(category));
    }

    @Override
    @Transactional
    public CategoryDto updateCategory(Long categoryId, AdminCategoryRequestDto request) {
        Category category = categoryRepository.findByIdForUpdate(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException(CATEGORY_NOT_FOUND_MESSAGE.formatted(categoryId)));
        String categoryName = UserInputNormalizer.normalizeName(request.name());
        requireCategoryNameAvailableForUpdate(categoryName, categoryId);
        category.setName(categoryName);

        return categoryMapper.toDto(saveCategoryOrThrowConflict(category));
    }

    @Override
    @Transactional
    public void updateCategoryOrder(CategoryOrderRequestDto categoryOrderRequest) {
        List<Long> requestedCategoryIds = requireCategoryIdsPresentAndUnique(categoryOrderRequest.categoryIds());
        List<Category> currentCategories = lockAllCategoriesForOrderUpdate();
        requireRequestedCategoryIdsMatchCurrentCategories(requestedCategoryIds, currentCategories);
        applyRequestedDisplayOrder(requestedCategoryIds, currentCategories);
        categoryRepository.flush();
    }

    private List<Category> lockAllCategoriesForOrderUpdate() {
        return categoryRepository.findAllForUpdateOrderByDisplayOrder();
    }

    private void requireRequestedCategoryIdsMatchCurrentCategories(
            List<Long> requestedCategoryIds,
            List<Category> currentCategories
    ) {
        Set<Long> currentCategoryIds = currentCategories.stream()
                .map(Category::getId)
                .collect(Collectors.toSet());

        if (!currentCategoryIds.equals(Set.copyOf(requestedCategoryIds))) {
            throw new BadRequestException(CATEGORY_ORDER_CURRENT_IDS_MESSAGE);
        }
    }

    private void applyRequestedDisplayOrder(List<Long> requestedCategoryIds, List<Category> currentCategories) {
        Map<Long, Category> categoriesById = currentCategories.stream()
                .collect(Collectors.toMap(Category::getId, category -> category));
        for (int index = 0; index < requestedCategoryIds.size(); index++) {
            categoriesById.get(requestedCategoryIds.get(index)).setDisplayOrder(index + 1);
        }
    }

    @Override
    @Transactional
    public void deleteCategory(Long categoryId, CategoryDeleteStrategy strategy, Long targetCategoryId) {
        requireDeleteStrategy(strategy);
        lockCategoryForDelete(categoryId, strategy, targetCategoryId);

        applyProductDeleteStrategy(categoryId, strategy, targetCategoryId);
        categoryRepository.deleteByIdDirect(categoryId);
        categoryRepository.flush();
    }

    private Category saveCategoryOrThrowConflict(Category category) {
        try {
            return categoryRepository.saveAndFlush(category);
        } catch (DataIntegrityViolationException exception) {
            if (isCategoryNameConstraintViolation(exception)) {
                throw new ConflictException(CATEGORY_ALREADY_EXISTS_MESSAGE);
            }
            throw exception;
        }
    }

    private void applyProductDeleteStrategy(
            Long categoryId,
            CategoryDeleteStrategy strategy,
            Long targetCategoryId
    ) {
        switch (strategy) {
            case KEEP_PRODUCTS -> keepProductsWithoutCategory(categoryId);
            case MOVE_PRODUCTS -> moveProductsToTargetCategory(categoryId, targetCategoryId);
            case DELETE_PRODUCTS -> deleteProductsAssignedToCategory(categoryId);
            default -> throw new BadRequestException(UNSUPPORTED_CATEGORY_DELETE_STRATEGY_MESSAGE);
        }
    }

    private void keepProductsWithoutCategory(Long categoryId) {
        homePageProductRepository.deleteByProductCategoryId(categoryId);
        productRepository.clearCategoryByCategoryId(categoryId);
    }

    private void moveProductsToTargetCategory(Long categoryId, Long targetCategoryId) {
        productRepository.moveCategoryByCategoryId(categoryId, targetCategoryId);
    }

    private void deleteProductsAssignedToCategory(Long categoryId) {
        List<Long> productIds = productRepository.findIdsByCategoryIdOrderById(categoryId);
        productDeletionService.deleteProductsByIdsIgnoringMissing(productIds);
    }

    private void requireDeleteStrategy(CategoryDeleteStrategy strategy) {
        if (strategy == null) {
            throw new BadRequestException(CATEGORY_DELETE_STRATEGY_REQUIRED_MESSAGE);
        }
    }

    private void lockCategoryForDelete(
            Long categoryId,
            CategoryDeleteStrategy strategy,
            Long targetCategoryId
    ) {
        if (strategy != CategoryDeleteStrategy.MOVE_PRODUCTS) {
            lockCategoryOrThrow(categoryId);
            return;
        }

        requireValidMoveTargetInput(categoryId, targetCategoryId);
        lockCategoryAndMoveTarget(categoryId, targetCategoryId);
    }

    private void lockCategoryAndMoveTarget(Long categoryId, Long targetCategoryId) {
        Long firstIdToLock = Math.min(categoryId, targetCategoryId);
        Long secondIdToLock = Math.max(categoryId, targetCategoryId);

        lockCategoryOrThrow(firstIdToLock);
        lockCategoryOrThrow(secondIdToLock);
    }

    private void lockCategoryOrThrow(Long categoryId) {
        categoryRepository.findByIdForUpdate(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException(CATEGORY_NOT_FOUND_MESSAGE.formatted(categoryId)));
    }

    private void requireValidMoveTargetInput(Long categoryId, Long targetCategoryId) {
        if (targetCategoryId == null) {
            throw new BadRequestException(MOVE_TARGET_REQUIRED_MESSAGE);
        }

        if (targetCategoryId.equals(categoryId)) {
            throw new BadRequestException(MOVE_TARGET_CANNOT_BE_DELETED_MESSAGE);
        }
    }

    private void requireCategoryNameAvailable(String name) {
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException(CATEGORY_ALREADY_EXISTS_MESSAGE);
        }
    }

    private int findNextDisplayOrder() {
        Integer highestDisplayOrder = categoryRepository.findMaxDisplayOrder();
        return highestDisplayOrder == null ? 1 : highestDisplayOrder + 1;
    }

    private List<Long> requireCategoryIdsPresentAndUnique(List<Long> requestedCategoryIds) {
        if (requestedCategoryIds == null) {
            throw new BadRequestException(CATEGORY_ORDER_IDS_REQUIRED_MESSAGE);
        }

        Set<Long> uniqueCategoryIds = new HashSet<>(requestedCategoryIds);
        if (uniqueCategoryIds.size() != requestedCategoryIds.size()) {
            throw new BadRequestException(CATEGORY_ORDER_IDS_UNIQUE_MESSAGE);
        }

        return List.copyOf(requestedCategoryIds);
    }

    private void requireCategoryNameAvailableForUpdate(String name, Long categoryId) {
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, categoryId)) {
            throw new ConflictException(CATEGORY_ALREADY_EXISTS_MESSAGE);
        }
    }

    private boolean isCategoryNameConstraintViolation(DataIntegrityViolationException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof org.hibernate.exception.ConstraintViolationException violation
                    && CATEGORY_NAME_UNIQUE_CONSTRAINT.equalsIgnoreCase(violation.getConstraintName())) {
                return true;
            }

            String message = cause.getMessage();
            if (message != null && message.contains(CATEGORY_NAME_UNIQUE_CONSTRAINT)) {
                return true;
            }

            cause = cause.getCause();
        }

        return false;
    }
}
