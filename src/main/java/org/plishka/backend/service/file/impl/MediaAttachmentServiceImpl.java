package org.plishka.backend.service.file.impl;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.plishka.backend.domain.media.MediaTargetType;
import org.plishka.backend.domain.media.MediaType;
import org.plishka.backend.event.media.MediaAttachedEvent;
import org.plishka.backend.exception.BadRequestException;
import org.plishka.backend.monitoring.metrics.StorageMetricsRecorder;
import org.plishka.backend.monitoring.transaction.TransactionalMetricsPublisher;
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
    private final StorageMetricsRecorder storageMetricsRecorder;
    private final TransactionalMetricsPublisher transactionalMetricsPublisher;

    public MediaAttachmentServiceImpl(
            List<MediaAttachmentHandler> handlers,
            MediaAttachmentValidator mediaAttachmentValidator,
            ApplicationEventPublisher applicationEventPublisher,
            StorageMetricsRecorder storageMetricsRecorder,
            TransactionalMetricsPublisher transactionalMetricsPublisher
    ) {
        handlersByTargetType = mapHandlersByTargetType(handlers);
        this.mediaAttachmentValidator = mediaAttachmentValidator;
        this.applicationEventPublisher = applicationEventPublisher;
        this.storageMetricsRecorder = storageMetricsRecorder;
        this.transactionalMetricsPublisher = transactionalMetricsPublisher;
    }

    @Override
    @Transactional
    public void attachMedia(MediaTargetType targetType, Long targetId, String s3Key) {
        MediaType mediaType = null;
        String outcome = StorageMetricsRecorder.OUTCOME_FAILURE;
        try {
            MediaAttachmentHandler handler = getRequiredHandler(targetType);
            ValidatedMediaAttachment attachment = mediaAttachmentValidator.validate(
                    s3Key,
                    targetType,
                    targetId,
                    handler.targetName()
            );
            mediaType = attachment.mediaType();

            requireMediaNotAttached(handler, attachment.s3Key(), targetId);
            attachValidatedMedia(handler, targetId, attachment);
            applicationEventPublisher.publishEvent(new MediaAttachedEvent(attachment.s3Key()));
            outcome = StorageMetricsRecorder.OUTCOME_SUCCESS;
        } finally {
            recordMediaAttach(targetType, mediaType, outcome);
        }
    }

    private void recordMediaAttach(MediaTargetType targetType, MediaType mediaType, String outcome) {
        if (!StorageMetricsRecorder.OUTCOME_SUCCESS.equals(outcome)) {
            storageMetricsRecorder.recordMediaAttach(targetType, mediaType, outcome);
            return;
        }

        transactionalMetricsPublisher.afterCompletionOrNow(
                () -> storageMetricsRecorder.recordMediaAttach(
                        targetType,
                        mediaType,
                        StorageMetricsRecorder.OUTCOME_SUCCESS
                ),
                () -> storageMetricsRecorder.recordMediaAttach(
                        targetType,
                        mediaType,
                        StorageMetricsRecorder.OUTCOME_FAILURE
                )
        );
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
            log.debug("Media is already attached to {}", handler.targetDescription(targetId));
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
                    "Failed to attach media because it conflicts with existing {} media",
                    handler.targetDescription(targetId),
                    exception
            );
            throw new BadRequestException("This media file is already attached or media order has changed.");
        }
    }
}
