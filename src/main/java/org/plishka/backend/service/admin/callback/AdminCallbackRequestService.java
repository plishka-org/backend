package org.plishka.backend.service.admin.callback;

import org.plishka.backend.dto.admin.callback.AdminCallbackRequestSearchRequestDto;
import org.plishka.backend.dto.callback.CallbackRequestDto;
import org.plishka.backend.dto.common.PageResponse;

public interface AdminCallbackRequestService {
    PageResponse<CallbackRequestDto> getCallbackRequests(
            AdminCallbackRequestSearchRequestDto searchRequest,
            int page,
            int size
    );
}
