package org.plishka.backend.dto.callback;

import java.time.Instant;

public record CallbackRequestDto(
        Long callbackRequestId,
        String name,
        String phone,
        String message,
        Instant createdAt
) {
}
