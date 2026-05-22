package org.plishka.backend.event.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.plishka.backend.event.media.MediaAttachedEvent;
import org.plishka.backend.service.storage.tagging.RetryableMediaStorageTagger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class MediaStorageEventListener {
    private final RetryableMediaStorageTagger retryableMediaStorageTagger;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMediaAttached(MediaAttachedEvent event) {
        try {
            retryableMediaStorageTagger.markObjectAsAttached(event.s3Key());
        } catch (RuntimeException exception) {
            log.error("Unexpected failure while marking media object as attached: s3Key={}", event.s3Key(), exception);
        }
    }
}
