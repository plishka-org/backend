package org.plishka.backend.service.callback;

import org.plishka.backend.dto.callback.CallbackRequestCreateDto;
import org.plishka.backend.dto.callback.CallbackRequestDto;
import org.plishka.backend.dto.common.PageResponse;

public interface CallbackRequestService {
    CallbackRequestDto createCallbackRequest(Long userId, CallbackRequestCreateDto request);

    PageResponse<CallbackRequestDto> getCallbackRequests(Long userId, int page, int size);
}
