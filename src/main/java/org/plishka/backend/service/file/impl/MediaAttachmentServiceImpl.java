package org.plishka.backend.service.file.impl;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.plishka.backend.domain.media.MediaTargetType;
import org.plishka.backend.event.media.MediaAttachedEvent;
import org.plishka.backend.exception.BadRequestException;
import org.plishka.backend.service.file.MediaAttachmentHandler;
import org.plishka.backend.service.file.MediaAttachmentService;
import org.plishka.backend.service.storage.validation.MediaAttachmentValidator;
import org.plishka.backend.service.storage.validation.MediaAttachmentValidator.ValidatedMediaAttachment;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class MediaAttachmentServiceImpl implements MediaAttachmentService {
    private final Map<MediaTargetType, MediaAttachmentHandler> handlersByTargetType;
    private final MediaAttachmentValidator mediaAttachmentValidator;
    private final ApplicationEventPublisher applicationEventPublisher;

    public MediaAttachmentServiceImpl(
            List<MediaAttachmentHandler> handlers,
            MediaAttachmentValidator mediaAttachmentValidator,
            ApplicationEventPublisher applicationEventPublisher
    ) {
        handlersByTargetType = mapHandlersByTargetType(handlers);
        this.mediaAttachmentValidator = mediaAttachmentValidator;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    @Transactional
    public void attachMedia(MediaTargetType targetType, Long targetId, String s3Key) {
        MediaAttachmentHandler handler = getRequiredHandler(targetType);
        ValidatedMediaAttachment attachment = mediaAttachmentValidator.validate(
                s3Key,
                targetType,
                targetId,
                handler.targetName()
        );

        requireMediaNotAttached(handler, attachment.s3Key(), targetId);
        attachValidatedMedia(handler, targetId, attachment);
        applicationEventPublisher.publishEvent(new MediaAttachedEvent(attachment.s3Key()));
    }

    private Map<MediaTargetType, MediaAttachmentHandler> mapHandlersByTargetType(
            List<MediaAttachmentHandler> handlers
    ) {
        Map<MediaTargetType, MediaAttachmentHandler> indexedHandlers = new EnumMap<>(MediaTargetType.class);
        handlers.forEach(handler -> registerHandler(indexedHandlers, handler));

        return Map.copyOf(indexedHandlers);
    }

    private void registerHandler(
            Map<MediaTargetType, MediaAttachmentHandler> handlersByTargetType,
            MediaAttachmentHandler handler
    ) {
        MediaAttachmentHandler previousHandler = handlersByTargetType.putIfAbsent(handler.targetType(), handler);
        if (previousHandler != null) {
            throw new IllegalStateException(
                    "Duplicate media attachment handler for target type: " + handler.targetType()
            );
        }
    }

    private MediaAttachmentHandler getRequiredHandler(MediaTargetType targetType) {
        MediaAttachmentHandler handler = handlersByTargetType.get(targetType);
        if (handler == null) {
            throw new BadRequestException("Unsupported media target type");
        }

        return handler;
    }

    private void requireMediaNotAttached(MediaAttachmentHandler handler, String s3Key, Long targetId) {
        if (handler.existsByS3Key(s3Key)) {
            log.debug("Media with S3 key {} is already attached to {}", s3Key, handler.targetDescription(targetId));
            throw new BadRequestException("This media file is already attached.");
        }
    }

    private void attachValidatedMedia(
            MediaAttachmentHandler handler,
            Long targetId,
            ValidatedMediaAttachment attachment
    ) {
        try {
            handler.attachValidatedMedia(targetId, attachment.s3Key(), attachment.mediaType());
        } catch (DataIntegrityViolationException exception) {
            log.debug(
                    "Failed to attach media {} because it conflicts with existing {} media",
                    attachment.s3Key(),
                    handler.targetDescription(targetId),
                    exception
            );
            throw new BadRequestException("This media file is already attached or media order has changed.");
        }
    }
}
