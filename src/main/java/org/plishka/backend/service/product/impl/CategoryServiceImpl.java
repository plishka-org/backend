package org.plishka.backend.service.product.impl;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.plishka.backend.dto.product.CategoryDto;
import org.plishka.backend.mapper.product.CategoryMapper;
import org.plishka.backend.repository.product.CategoryRepository;
import org.plishka.backend.service.product.CategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDto> getCategories() {
        log.debug("Fetching categories");

        List<CategoryDto> result = categoryRepository.findAllByOrderByDisplayOrderAscIdAsc()
                .stream()
                .map(categoryMapper::toDto)
                .toList();

        log.debug("Successfully fetched {} categories", result.size());

        return result;
    }
}
