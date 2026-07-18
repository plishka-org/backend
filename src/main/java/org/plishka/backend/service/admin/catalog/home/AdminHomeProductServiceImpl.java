package org.plishka.backend.service.admin.catalog.home;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.domain.home.HomePageProduct;
import org.plishka.backend.domain.product.Product;
import org.plishka.backend.dto.admin.home.HomeProductsRequestDto;
import org.plishka.backend.exception.BadRequestException;
import org.plishka.backend.repository.home.HomePageProductRepository;
import org.plishka.backend.repository.product.ProductRepository;
import org.plishka.backend.util.EntityPresenceValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminHomeProductServiceImpl implements AdminHomeProductService {
    private final HomePageProductRepository homePageProductRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public void replaceHomeProducts(HomeProductsRequestDto request) {
        List<Long> productIds = requireUniqueIds(request.productIds());
        Map<Long, Product> productsById = findLockedProductsWithCategoryByIdOrThrow(productIds);
        requireProductsCategorized(productsById.values());

        replaceHomeRows(productIds, productsById);
    }

    @Override
    @Transactional
    public void reorderHomeProducts(HomeProductsRequestDto request) {
        List<Long> productIds = requireUniqueIds(request.productIds());
        List<HomePageProduct> currentRows = homePageProductRepository.findAllForUpdateOrderByDisplayOrder();
        Set<Long> currentIds = currentRows.stream()
                .map(row -> row.getProduct().getId())
                .collect(Collectors.toSet());

        if (!currentIds.equals(Set.copyOf(productIds))) {
            throw new BadRequestException("Home product order must contain the current home product ids");
        }

        Map<Long, Product> productsById = findLockedProductsWithCategoryByIdOrThrow(productIds);
        requireProductsCategorized(productsById.values());

        replaceHomeRows(productIds, productsById);
    }

    private void replaceHomeRows(List<Long> productIds, Map<Long, Product> productsById) {
        homePageProductRepository.deleteAllInBatch();
        homePageProductRepository.flush();

        for (int index = 0; index < productIds.size(); index++) {
            HomePageProduct row = new HomePageProduct();
            row.setProduct(productsById.get(productIds.get(index)));
            row.setDisplayOrder(index + 1);
            homePageProductRepository.save(row);
        }

        homePageProductRepository.flush();
    }

    private Map<Long, Product> findLockedProductsWithCategoryByIdOrThrow(List<Long> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }

        productRepository.findAllByIdInForUpdateOrderById(productIds);
        List<Product> products = productRepository.findAllByIdInWithCategoryOrderById(productIds);
        EntityPresenceValidator.requireAllIdsFound(
                productIds,
                products.stream().map(Product::getId).toList(),
                "Product"
        );

        return products.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
    }

    private void requireProductsCategorized(Iterable<Product> products) {
        for (Product product : products) {
            if (product.getCategory() == null) {
                throw new BadRequestException("Products without category cannot be shown on the home page");
            }
        }
    }

    private List<Long> requireUniqueIds(List<Long> productIds) {
        if (productIds == null) {
            throw new BadRequestException("Product ids are required");
        }

        if (productIds.stream().anyMatch(id -> id == null)) {
            throw new BadRequestException("Product ids must not contain null values");
        }

        Set<Long> uniqueIds = new HashSet<>(productIds);
        if (uniqueIds.size() != productIds.size()) {
            throw new BadRequestException("Product ids must be unique");
        }

        return List.copyOf(productIds);
    }
}
