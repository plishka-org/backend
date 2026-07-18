package org.plishka.backend.event.listener;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.plishka.backend.event.media.MediaAttachedEvent;
import org.plishka.backend.monitoring.sentry.SentryMonitoringService;
import org.plishka.backend.service.storage.tagging.RetryableMediaStorageTagger;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MediaStorageEventListenerTest {
    private static final String S3_KEY = "products/10/images/2026/05/image.jpg";

    @Mock
    private RetryableMediaStorageTagger retryableMediaStorageTagger;

    @Mock
    private SentryMonitoringService sentryMonitoringService;

    @Test
    void handleMediaAttached_ShouldRunAfterCommit() throws NoSuchMethodException {
        Method method = MediaStorageEventListener.class.getMethod("handleMediaAttached", MediaAttachedEvent.class);

        TransactionalEventListener annotation = method.getAnnotation(TransactionalEventListener.class);

        assertNotNull(annotation);
        assertEquals(TransactionPhase.AFTER_COMMIT, annotation.phase());
    }

    @Test
    void handleMediaAttached_ShouldMarkObjectAsAttached() {
        MediaStorageEventListener listener = listener();

        listener.handleMediaAttached(new MediaAttachedEvent(S3_KEY));

        verify(retryableMediaStorageTagger).markObjectAsAttached(S3_KEY);
    }

    @Test
    void handleMediaAttached_ShouldCaptureAndSwallowTaggingFailures() {
        RuntimeException failure = new RuntimeException("tagging failed");
        doThrow(failure).when(retryableMediaStorageTagger).markObjectAsAttached(S3_KEY);
        MediaStorageEventListener listener = listener();

        assertDoesNotThrow(() -> listener.handleMediaAttached(new MediaAttachedEvent(S3_KEY)));

        verify(sentryMonitoringService).captureException(failure, "storage", "mark_media_attached");
    }

    private MediaStorageEventListener listener() {
        return new MediaStorageEventListener(retryableMediaStorageTagger, sentryMonitoringService);
    }
}
