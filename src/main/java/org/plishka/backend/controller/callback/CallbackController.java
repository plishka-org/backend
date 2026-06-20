package org.plishka.backend.controller.callback;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.dto.callback.CallbackRequestCreateDto;
import org.plishka.backend.dto.callback.CallbackRequestDto;
import org.plishka.backend.dto.common.PageResponse;
import org.plishka.backend.dto.common.PaginationRequestDto;
import org.plishka.backend.security.AuthenticatedUserPrincipal;
import org.plishka.backend.service.callback.CallbackRequestService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CallbackController {
    private static final int DEFAULT_PAGE_SIZE = 10;

    private final CallbackRequestService callbackRequestService;

    @PostMapping("/callback")
    @ResponseStatus(HttpStatus.CREATED)
    public CallbackRequestDto createCallbackRequest(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Valid @RequestBody CallbackRequestCreateDto request
    ) {
        return callbackRequestService.createCallbackRequest(principal.getUserId(), request);
    }

    @GetMapping("/users/me/callback-requests")
    public PageResponse<CallbackRequestDto> getCallbackRequests(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Valid @ModelAttribute PaginationRequestDto paginationRequest
    ) {
        return callbackRequestService.getCallbackRequests(
                principal.getUserId(),
                paginationRequest.resolvePage(),
                paginationRequest.resolveSize(DEFAULT_PAGE_SIZE)
        );
    }
}
