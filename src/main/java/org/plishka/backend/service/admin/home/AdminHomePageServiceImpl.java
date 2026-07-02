package org.plishka.backend.service.admin.home;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.domain.home.HomePageAdvantage;
import org.plishka.backend.domain.home.HomePageContent;
import org.plishka.backend.dto.admin.home.AdminHomePageAdvantageDto;
import org.plishka.backend.dto.admin.home.AdminHomePageAdvantageRequestDto;
import org.plishka.backend.dto.admin.home.AdminHomePageContentRequestDto;
import org.plishka.backend.dto.admin.home.AdminHomePageDto;
import org.plishka.backend.dto.admin.home.HomeAdvantagesOrderRequestDto;
import org.plishka.backend.dto.home.HomePageContentDto;
import org.plishka.backend.exception.BadRequestException;
import org.plishka.backend.exception.ResourceNotFoundException;
import org.plishka.backend.mapper.home.HomePageMapper;
import org.plishka.backend.repository.home.HomePageAdvantageRepository;
import org.plishka.backend.repository.home.HomePageContentRepository;
import org.plishka.backend.service.storage.StorageDeletionOutboxService;
import org.plishka.backend.util.EntityPresenceValidator;
import org.plishka.backend.util.UserInputNormalizer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AdminHomePageServiceImpl implements AdminHomePageService {
    private static final long SINGLETON_CONTENT_ID = 1L;
    private static final int MAX_ADVANTAGES = 20;
    private static final String CONTENT_NOT_FOUND_MESSAGE = "Home page content not found";
    private static final String ADVANTAGE_NOT_FOUND_MESSAGE = "Home page advantage with ID %d not found";
    private static final String ADVANTAGE_LIMIT_MESSAGE = "Home page cannot contain more than %d advantages";
    private static final String ADVANTAGE_ORDER_MISMATCH_MESSAGE =
            "Advantage order must contain the current home page advantage ids";

    private final HomePageContentRepository contentRepository;
    private final HomePageAdvantageRepository advantageRepository;
    private final HomePageMapper homePageMapper;
    private final StorageDeletionOutboxService storageDeletionOutboxService;

    @Override
    @Transactional(readOnly = true)
    public AdminHomePageDto getHomePage() {
        HomePageContent content = findContentOrThrow();
        List<AdminHomePageAdvantageDto> advantages = advantageRepository.findAllByOrderByDisplayOrderAsc()
                .stream()
                .map(homePageMapper::toAdminAdvantageDto)
                .toList();

        return new AdminHomePageDto(homePageMapper.toContentDto(content), advantages);
    }

    @Override
    @Transactional
    public HomePageContentDto updateHomePageContent(AdminHomePageContentRequestDto request) {
        HomePageContent content = findContentForUpdateOrThrow();
        content.setTitle(UserInputNormalizer.normalizeName(request.title()));
        content.setDescription(normalizeOptionalText(request.description()));

        return homePageMapper.toContentDto(contentRepository.saveAndFlush(content));
    }

    @Override
    @Transactional
    public AdminHomePageAdvantageDto createAdvantage(AdminHomePageAdvantageRequestDto request) {
        findContentForUpdateOrThrow();
        List<HomePageAdvantage> existingAdvantages = loadAdvantagesForUpdateOrThrowCapacity();

        HomePageAdvantage advantage = new HomePageAdvantage();
        applyAdvantageState(advantage, request);
        advantage.setDisplayOrder(resolveNextDisplayOrder(existingAdvantages));

        return homePageMapper.toAdminAdvantageDto(advantageRepository.saveAndFlush(advantage));
    }

    @Override
    @Transactional
    public AdminHomePageAdvantageDto updateAdvantage(Long advantageId, AdminHomePageAdvantageRequestDto request) {
        HomePageAdvantage advantage = findAdvantageForUpdateOrThrow(advantageId);
        String previousIconS3Key = advantage.getIconS3Key();

        applyAdvantageState(advantage, request);
        AdminHomePageAdvantageDto updatedAdvantage =
                homePageMapper.toAdminAdvantageDto(advantageRepository.saveAndFlush(advantage));

        enqueueIconDeletionIfReplaced(previousIconS3Key, advantage.getIconS3Key());

        return updatedAdvantage;
    }

    @Override
    @Transactional
    public void deleteAdvantage(Long advantageId) {
        HomePageAdvantage advantage = findAdvantageForUpdateOrThrow(advantageId);
        String iconS3Key = advantage.getIconS3Key();

        advantageRepository.delete(advantage);
        advantageRepository.flush();

        enqueueIconDeletionIfPresent(iconS3Key);
    }

    @Override
    @Transactional
    public void reorderAdvantages(HomeAdvantagesOrderRequestDto request) {
        List<Long> advantageIds = requireUniqueAdvantageIds(request.advantageIds());
        List<HomePageAdvantage> currentAdvantages = advantageRepository.findAllForUpdateOrderByDisplayOrder();
        Set<Long> currentIds = currentAdvantages.stream()
                .map(HomePageAdvantage::getId)
                .collect(Collectors.toSet());

        if (!currentIds.equals(Set.copyOf(advantageIds))) {
            throw new BadRequestException(ADVANTAGE_ORDER_MISMATCH_MESSAGE);
        }

        if (advantageIds.isEmpty()) {
            return;
        }

        Map<Long, HomePageAdvantage> advantagesById = currentAdvantages.stream()
                .collect(Collectors.toMap(HomePageAdvantage::getId, Function.identity()));
        EntityPresenceValidator.requireAllIdsFound(advantageIds, advantagesById.keySet(), "Home page advantage");

        applyTemporaryDisplayOrders(advantageIds, advantagesById);
        applyFinalDisplayOrders(advantageIds, advantagesById);
        advantageRepository.flush();
    }

    private void applyTemporaryDisplayOrders(List<Long> advantageIds, Map<Long, HomePageAdvantage> advantagesById) {
        for (int index = 0; index < advantageIds.size(); index++) {
            HomePageAdvantage advantage = advantagesById.get(advantageIds.get(index));
            advantage.setDisplayOrder(-(index + 1));
        }
        advantageRepository.flush();
    }

    private void applyFinalDisplayOrders(List<Long> advantageIds, Map<Long, HomePageAdvantage> advantagesById) {
        for (int index = 0; index < advantageIds.size(); index++) {
            HomePageAdvantage advantage = advantagesById.get(advantageIds.get(index));
            advantage.setDisplayOrder(index + 1);
        }
    }

    private List<HomePageAdvantage> loadAdvantagesForUpdateOrThrowCapacity() {
        List<HomePageAdvantage> advantages = advantageRepository.findAllForUpdateOrderByDisplayOrder();
        if (advantages.size() >= MAX_ADVANTAGES) {
            throw new BadRequestException(ADVANTAGE_LIMIT_MESSAGE.formatted(MAX_ADVANTAGES));
        }

        return advantages;
    }

    private int resolveNextDisplayOrder(List<HomePageAdvantage> advantages) {
        return advantages.stream()
                .mapToInt(HomePageAdvantage::getDisplayOrder)
                .max()
                .orElse(0) + 1;
    }

    private void applyAdvantageState(HomePageAdvantage advantage, AdminHomePageAdvantageRequestDto request) {
        advantage.setTitle(UserInputNormalizer.normalizeName(request.title()));
        advantage.setDescription(normalizeOptionalText(request.description()));
        advantage.setIconS3Key(normalizeIconS3Key(request.iconS3Key()));
    }

    private String normalizeOptionalText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        return UserInputNormalizer.normalizeName(value);
    }

    private String normalizeIconS3Key(String iconS3Key) {
        if (!StringUtils.hasText(iconS3Key)) {
            return null;
        }

        return iconS3Key.trim();
    }

    private void enqueueIconDeletionIfReplaced(String previousIconS3Key, String currentIconS3Key) {
        if (!StringUtils.hasText(previousIconS3Key)) {
            return;
        }

        if (previousIconS3Key.equals(currentIconS3Key)) {
            return;
        }

        storageDeletionOutboxService.enqueueDelete(previousIconS3Key);
    }

    private void enqueueIconDeletionIfPresent(String iconS3Key) {
        if (StringUtils.hasText(iconS3Key)) {
            storageDeletionOutboxService.enqueueDelete(iconS3Key);
        }
    }

    private List<Long> requireUniqueAdvantageIds(List<Long> advantageIds) {
        if (advantageIds == null) {
            throw new BadRequestException("Advantage ids are required");
        }

        if (advantageIds.stream().anyMatch(Objects::isNull)) {
            throw new BadRequestException("Advantage ids must not contain null values");
        }

        Set<Long> uniqueIds = new HashSet<>(advantageIds);
        if (uniqueIds.size() != advantageIds.size()) {
            throw new BadRequestException("Advantage ids must be unique");
        }

        return List.copyOf(advantageIds);
    }

    private HomePageContent findContentOrThrow() {
        return contentRepository.findById(SINGLETON_CONTENT_ID)
                .orElseThrow(() -> new ResourceNotFoundException(CONTENT_NOT_FOUND_MESSAGE));
    }

    private HomePageContent findContentForUpdateOrThrow() {
        return contentRepository.findByIdForUpdate(SINGLETON_CONTENT_ID)
                .orElseThrow(() -> new ResourceNotFoundException(CONTENT_NOT_FOUND_MESSAGE));
    }

    private HomePageAdvantage findAdvantageForUpdateOrThrow(Long advantageId) {
        return advantageRepository.findByIdForUpdate(advantageId)
                .orElseThrow(() -> new ResourceNotFoundException(ADVANTAGE_NOT_FOUND_MESSAGE.formatted(advantageId)));
    }
}
