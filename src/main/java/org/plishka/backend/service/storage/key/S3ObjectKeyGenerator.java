package org.plishka.backend.service.storage.key;

import java.time.Clock;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.domain.media.MediaTargetType;
import org.plishka.backend.domain.media.MediaType;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class S3ObjectKeyGenerator {
    private final Clock clock;

    public String generate(
            MediaTargetType targetType,
            Long targetId,
            MediaType mediaType,
            String extension
    ) {
        YearMonth yearMonth = YearMonth.from(clock.instant().atZone(ZoneOffset.UTC));

        return "%s/%d/%s/%d/%02d/%s.%s".formatted(
                targetType.getKeySegment(),
                targetId,
                mediaType.getKeyFolder(),
                yearMonth.getYear(),
                yearMonth.getMonthValue(),
                UUID.randomUUID(),
                extension
        );
    }
}
