package org.plishka.backend.service.callback.impl;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.plishka.backend.domain.callback.CallbackRequest;
import org.plishka.backend.domain.user.User;
import org.plishka.backend.dto.callback.CallbackRequestCreateDto;
import org.plishka.backend.dto.callback.CallbackRequestDto;
import org.plishka.backend.dto.common.PageResponse;
import org.plishka.backend.event.callback.CallbackRequestCreatedEvent;
import org.plishka.backend.mapper.callback.CallbackRequestMapper;
import org.plishka.backend.monitoring.metrics.BusinessMetricsRecorder;
import org.plishka.backend.monitoring.transaction.TransactionalMetricsPublisher;
import org.plishka.backend.repository.callback.CallbackRequestRepository;
import org.plishka.backend.service.callback.CallbackRequestService;
import org.plishka.backend.service.user.EligibleUserProvider;
import org.plishka.backend.util.UserInputNormalizer;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CallbackRequestServiceImpl implements CallbackRequestService {
    private static final String OUTCOME_FAILURE = "failure";
    private static final String OUTCOME_SUCCESS = "success";

    private static final Sort CALLBACK_REQUESTS_SORT = Sort.by(
            Sort.Order.desc("createdAt"),
            Sort.Order.desc("id")
    );

    private final CallbackRequestRepository callbackRequestRepository;
    private final EligibleUserProvider eligibleUserProvider;
    private final CallbackRequestMapper callbackRequestMapper;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final BusinessMetricsRecorder businessMetricsRecorder;
    private final TransactionalMetricsPublisher transactionalMetricsPublisher;

    @Override
    @Transactional
    public CallbackRequestDto createCallbackRequest(Long userId, CallbackRequestCreateDto request) {
        try {
            User user = eligibleUserProvider.getEligibleUserOrThrow(userId);

            CallbackRequest callbackRequest = CallbackRequest.builder()
                    .user(user)
                    .name(UserInputNormalizer.normalizeName(request.name()))
                    .phone(UserInputNormalizer.normalizePhone(request.phone()))
                    .message(request.message().trim())
                    .build();

            CallbackRequest savedRequest = callbackRequestRepository.saveAndFlush(callbackRequest);
            applicationEventPublisher.publishEvent(CallbackRequestCreatedEvent.builder()
                    .callbackRequestId(savedRequest.getId())
                    .userId(user.getId())
                    .userEmail(user.getEmail())
                    .name(savedRequest.getName())
                    .phone(savedRequest.getPhone())
                    .message(savedRequest.getMessage())
                    .createdAt(savedRequest.getCreatedAt())
                    .build());

            transactionalMetricsPublisher.afterCompletionOrNow(
                    () -> businessMetricsRecorder.recordCallbackRequest(OUTCOME_SUCCESS),
                    () -> businessMetricsRecorder.recordCallbackRequest(OUTCOME_FAILURE)
            );

            log.info("Callback request created: callbackRequestId={}, userId={}", savedRequest.getId(), userId);

            return callbackRequestMapper.toDto(savedRequest);
        } catch (RuntimeException exception) {
            businessMetricsRecorder.recordCallbackRequest(OUTCOME_FAILURE);
            throw exception;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CallbackRequestDto> getCallbackRequests(Long userId, int page, int size) {
        Page<CallbackRequest> callbackRequestsPage = callbackRequestRepository.findAllByUser_Id(
                userId,
                PageRequest.of(page, size, CALLBACK_REQUESTS_SORT)
        );
        List<CallbackRequestDto> content = callbackRequestsPage.getContent()
                .stream()
                .map(callbackRequestMapper::toDto)
                .toList();

        return PageResponse.from(callbackRequestsPage, content);
    }
}
