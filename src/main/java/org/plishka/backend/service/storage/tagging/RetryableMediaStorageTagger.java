package org.plishka.backend.service.storage.tagging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.plishka.backend.exception.StorageOperationException;
import org.plishka.backend.service.storage.ObjectStorageService;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RetryableMediaStorageTagger {
    private final ObjectStorageService objectStorageService;

    @Retryable(
            retryFor = StorageOperationException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 200, multiplier = 2.0)
    )
    public void markObjectAsAttached(String s3Key) {
        objectStorageService.markObjectAsAttached(s3Key);

        log.info("Media object marked as attached in storage: s3Key={}", s3Key);
    }

    @Recover
    public void recover(StorageOperationException exception, String s3Key) {
        throw exception;
    }
}
