package org.plishka.backend.service.file.impl;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.plishka.backend.domain.media.MediaTargetType;
import org.plishka.backend.domain.media.MediaType;
import org.plishka.backend.event.media.MediaAttachedEvent;
import org.plishka.backend.exception.BadRequestException;
import org.plishka.backend.monitoring.metrics.StorageMetricsRecorder;
import org.plishka.backend.monitoring.transaction.TransactionalMetricsPublisher;
import org.plishka.backend.service.file.MediaAttachmentHandler;
import org.plishka.backend.service.storage.validation.MediaAttachmentValidator;
import org.plishka.backend.service.storage.validation.MediaAttachmentValidator.ValidatedMediaAttachment;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MediaAttachmentServiceImplTest {
    private static final MediaTargetType TARGET_TYPE = MediaTargetType.PRODUCT;
    private static final Long TARGET_ID = 10L;
    private static final String RAW_S3_KEY = " products/10/images/2026/05/image.jpg ";
    private static final String VALIDATED_S3_KEY = "products/10/images/2026/05/image.jpg";

    @Mock
    private MediaAttachmentHandler handler;

    @Mock
    private MediaAttachmentValidator mediaAttachmentValidator;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private StorageMetricsRecorder storageMetricsRecorder;

    @Mock
    private TransactionalMetricsPublisher transactionalMetricsPublisher;

    private MediaAttachmentServiceImpl service;

    @BeforeEach
    void setUp() {
        when(handler.targetType()).thenReturn(TARGET_TYPE);
        when(handler.targetName()).thenReturn("product");
        service = new MediaAttachmentServiceImpl(
                List.of(handler),
                mediaAttachmentValidator,
                applicationEventPublisher,
                storageMetricsRecorder,
                transactionalMetricsPublisher
        );
    }

    @Test
    void attachMedia_ShouldAttachValidatedMediaPublishEventAndRecordSuccessAfterCommit() {
        when(mediaAttachmentValidator.validate(RAW_S3_KEY, TARGET_TYPE, TARGET_ID, "product"))
                .thenReturn(new ValidatedMediaAttachment(VALIDATED_S3_KEY, MediaType.IMAGE));
        when(handler.existsByS3Key(VALIDATED_S3_KEY)).thenReturn(false);
        doAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return null;
        }).when(transactionalMetricsPublisher).afterCompletionOrNow(any(Runnable.class), any(Runnable.class));

        service.attachMedia(TARGET_TYPE, TARGET_ID, RAW_S3_KEY);

        verify(handler).attachValidatedMedia(TARGET_ID, VALIDATED_S3_KEY, MediaType.IMAGE);

        ArgumentCaptor<MediaAttachedEvent> eventCaptor = ArgumentCaptor.forClass(MediaAttachedEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        assertEquals(VALIDATED_S3_KEY, eventCaptor.getValue().s3Key());

        verify(storageMetricsRecorder).recordMediaAttach(
                TARGET_TYPE,
                MediaType.IMAGE,
                StorageMetricsRecorder.OUTCOME_SUCCESS
        );
    }

    @Test
    void attachMedia_ShouldRejectDuplicateAndNotPublishEvent() {
        when(mediaAttachmentValidator.validate(RAW_S3_KEY, TARGET_TYPE, TARGET_ID, "product"))
                .thenReturn(new ValidatedMediaAttachment(VALIDATED_S3_KEY, MediaType.IMAGE));
        when(handler.existsByS3Key(VALIDATED_S3_KEY)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> service.attachMedia(TARGET_TYPE, TARGET_ID, RAW_S3_KEY));

        verify(handler, never()).attachValidatedMedia(any(), any(), any());
        verify(applicationEventPublisher, never()).publishEvent(any());
        verify(transactionalMetricsPublisher, never()).afterCompletionOrNow(any(), any());
        verify(storageMetricsRecorder).recordMediaAttach(
                TARGET_TYPE,
                MediaType.IMAGE,
                StorageMetricsRecorder.OUTCOME_FAILURE
        );
    }
}
