package org.plishka.backend.controller.callback;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.dto.callback.CallbackRequestCreateDto;
import org.plishka.backend.dto.callback.CallbackRequestDto;
import org.plishka.backend.dto.common.PageResponse;
import org.plishka.backend.dto.common.PaginationRequestDto;
import org.plishka.backend.security.AuthenticatedUserPrincipal;
import org.plishka.backend.service.callback.CallbackRequestService;
import org.springdoc.core.annotations.ParameterObject;
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
@Tag(name = "Callback")
@SecurityRequirement(name = "bearerAuth")
public class CallbackController {
    private static final int DEFAULT_PAGE_SIZE = 10;

    private final CallbackRequestService callbackRequestService;

    @Operation(
            operationId = "createCallbackRequest",
            summary = "Create callback request",
            description = "Requires an active user: authenticated, email verified, and not banned."
    )
    @ApiResponse(responseCode = "201", description = "Callback request created.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(examples = @ExampleObject(value = """
                    {
                      "name": "Olena Shevchenko",
                      "phone": "+380501234567",
                      "message": "Please call me back about my order"
                    }
                    """))
    )
    @PostMapping("/callback")
    @ResponseStatus(HttpStatus.CREATED)
    public CallbackRequestDto createCallbackRequest(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Valid @RequestBody CallbackRequestCreateDto request
    ) {
        return callbackRequestService.createCallbackRequest(principal.getUserId(), request);
    }

    @Operation(
            operationId = "getCallbackRequests",
            summary = "List current user callback requests",
            description = "Requires an active user. Pagination defaults: page=0, size=10."
    )
    @GetMapping("/users/me/callback-requests")
    public PageResponse<CallbackRequestDto> getCallbackRequests(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @ParameterObject @Valid @ModelAttribute PaginationRequestDto paginationRequest
    ) {
        return callbackRequestService.getCallbackRequests(
                principal.getUserId(),
                paginationRequest.resolvePage(),
                paginationRequest.resolveSize(DEFAULT_PAGE_SIZE)
        );
    }
}
