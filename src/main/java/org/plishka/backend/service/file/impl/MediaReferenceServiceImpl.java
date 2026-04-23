package org.plishka.backend.service.file.impl;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.plishka.backend.domain.media.MediaTargetType;
import org.plishka.backend.exception.BadRequestException;
import org.plishka.backend.service.file.MediaReferenceHandler;
import org.plishka.backend.service.file.MediaReferenceService;
import org.springframework.stereotype.Service;

@Service
public class MediaReferenceServiceImpl implements MediaReferenceService {
    private final Map<MediaTargetType, MediaReferenceHandler> handlersByTargetType;

    public MediaReferenceServiceImpl(List<MediaReferenceHandler> handlers) {
        handlersByTargetType = mapHandlersByTargetType(handlers);
    }

    @Override
    public void assertParentExists(MediaTargetType targetType, Long targetId) {
        if (handlersByTargetType.isEmpty()) {
            return;
        }

        getRequiredHandler(targetType).assertParentExists(targetId);
    }

    @Override
    public boolean isAttached(String s3Key) {
        return findHandlerByS3Key(s3Key)
                .map(handler -> handler.existsByS3Key(s3Key))
                .orElse(false);
    }

    private Map<MediaTargetType, MediaReferenceHandler> mapHandlersByTargetType(List<MediaReferenceHandler> handlers) {
        Map<MediaTargetType, MediaReferenceHandler> indexedHandlers = new EnumMap<>(MediaTargetType.class);
        handlers.forEach(handler -> registerHandler(indexedHandlers, handler));

        return Map.copyOf(indexedHandlers);
    }

    private void registerHandler(
            Map<MediaTargetType, MediaReferenceHandler> handlersByTargetType,
            MediaReferenceHandler handler
    ) {
        MediaReferenceHandler previousHandler = handlersByTargetType.putIfAbsent(handler.targetType(), handler);
        if (previousHandler != null) {
            throw new IllegalStateException(
                    "Duplicate media reference handler for target type: " + handler.targetType()
            );
        }
    }

    private MediaReferenceHandler getRequiredHandler(MediaTargetType targetType) {
        MediaReferenceHandler handler = handlersByTargetType.get(targetType);
        if (handler == null) {
            throw new BadRequestException("Unsupported media target type");
        }

        return handler;
    }

    private Optional<MediaReferenceHandler> findHandlerByS3Key(String s3Key) {
        return extractTargetTypeFromS3Key(s3Key)
                .map(handlersByTargetType::get);
    }

    private Optional<MediaTargetType> extractTargetTypeFromS3Key(String s3Key) {
        int firstPathSeparatorIndex = s3Key.indexOf('/');
        if (firstPathSeparatorIndex <= 0) {
            return Optional.empty();
        }

        String leadingPathSegment = s3Key.substring(0, firstPathSeparatorIndex);
        return MediaTargetType.fromKeySegment(leadingPathSegment);
    }
}
