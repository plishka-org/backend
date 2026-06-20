package org.plishka.backend.event.callback;

import java.time.Instant;
import lombok.Builder;

@Builder
public record CallbackRequestCreatedEvent(
        Long callbackRequestId,
        Long userId,
        String name,
        String phone,
        String message,
        Instant createdAt
) {
}
