package org.plishka.backend.service.admin.callback;

import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.domain.callback.CallbackRequest;
import org.plishka.backend.dto.admin.callback.AdminCallbackRequestSearchRequestDto;
import org.plishka.backend.dto.callback.CallbackRequestDto;
import org.plishka.backend.dto.common.PageResponse;
import org.plishka.backend.mapper.callback.CallbackRequestMapper;
import org.plishka.backend.repository.callback.CallbackRequestRepository;
import org.plishka.backend.util.LikePatternEscaper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AdminCallbackRequestServiceImpl implements AdminCallbackRequestService {
    private final CallbackRequestRepository callbackRequestRepository;
    private final CallbackRequestMapper callbackRequestMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CallbackRequestDto> getCallbackRequests(
            AdminCallbackRequestSearchRequestDto searchRequest,
            int page,
            int size
    ) {
        String searchQuery = normalizeSearchQuery(searchRequest.search());
        String searchPattern = createCaseInsensitiveSearchPattern(searchQuery);
        String normalizedPhoneSearchPattern = createNormalizedPhoneSearchPattern(searchQuery);
        Page<CallbackRequest> callbackRequestPage = callbackRequestRepository.findPageForAdminSearch(
                searchPattern,
                normalizedPhoneSearchPattern,
                PageRequest.of(page, size, AdminCallbackRequestSortResolver.resolveSort(searchRequest.sort()))
        );
        List<CallbackRequestDto> callbackRequestDtos = callbackRequestPage.getContent().stream()
                .map(callbackRequestMapper::toDto)
                .toList();
        return PageResponse.from(callbackRequestPage, callbackRequestDtos);
    }

    private String normalizeSearchQuery(String rawSearchQuery) {
        return StringUtils.hasText(rawSearchQuery) ? rawSearchQuery.trim() : null;
    }

    private String createCaseInsensitiveSearchPattern(String searchQuery) {
        return searchQuery == null
                ? null
                : LikePatternEscaper.containsPattern(searchQuery.toLowerCase(Locale.ROOT));
    }

    private String createNormalizedPhoneSearchPattern(String searchQuery) {
        String normalizedPhoneSearchQuery = normalizePhoneSearchQuery(searchQuery);
        return normalizedPhoneSearchQuery == null
                ? null
                : LikePatternEscaper.containsPattern(normalizedPhoneSearchQuery);
    }

    private String normalizePhoneSearchQuery(String searchQuery) {
        return searchQuery == null
                ? null
                : searchQuery.replace(" ", "").replace("-", "");
    }
}
