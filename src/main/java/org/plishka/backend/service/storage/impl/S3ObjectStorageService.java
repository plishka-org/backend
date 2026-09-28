package org.plishka.backend.service.storage.impl;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.config.properties.StorageProperties;
import org.plishka.backend.domain.media.MediaTargetType;
import org.plishka.backend.exception.ResourceNotFoundException;
import org.plishka.backend.exception.StorageOperationException;
import org.plishka.backend.monitoring.metrics.StorageMetricsRecorder;
import org.plishka.backend.service.storage.ObjectStorageService;
import org.plishka.backend.service.storage.PresignedDownloadUrlCache;
import org.plishka.backend.service.storage.model.PresignedStorageUrl;
import org.plishka.backend.service.storage.model.StorageObjectMetadata;
import org.plishka.backend.service.storage.validation.S3ObjectKeyValidator;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.GetObjectTaggingRequest;
import software.amazon.awssdk.services.s3.model.GetObjectTaggingResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectTaggingRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
@RequiredArgsConstructor
public class S3ObjectStorageService implements ObjectStorageService {
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String CHECKSUM_SHA256_HEADER = "x-amz-checksum-sha256";
    private static final String TAGGING_HEADER = "x-amz-tagging";
    private static final String UPLOAD_STATUS_TAG_KEY = "upload-status";
    private static final String PENDING_UPLOAD_STATUS = "pending";
    private static final String ATTACHED_UPLOAD_STATUS = "attached";
    private static final String PENDING_UPLOAD_TAGGING = UPLOAD_STATUS_TAG_KEY + "=" + PENDING_UPLOAD_STATUS;
    private static final int DELETE_OBJECTS_BATCH_SIZE = 1000;
    private static final int S3_NOT_FOUND_STATUS = 404;
    private static final List<String> CLEANUP_PREFIXES = Arrays.stream(MediaTargetType.values())
            .map(targetType -> targetType.getKeySegment() + "/")
            .toList();

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final StorageProperties storageProperties;
    private final S3ObjectKeyValidator s3ObjectKeyValidator;
    private final Clock clock;
    private final StorageMetricsRecorder storageMetricsRecorder;
    private final PresignedDownloadUrlCache presignedDownloadUrlCache;

    @Override
    public PresignedStorageUrl presignUpload(String s3Key, String contentType, String checksumSha256Base64) {
        String normalizedKey = s3ObjectKeyValidator.validateAndNormalizeS3Key(s3Key);
        Duration ttl = storageProperties.s3().uploadPresignTtl();
        String bucket = storageProperties.s3().bucket();

        return recordS3Operation("presign_upload", () -> {
            try {
                PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(
                        PutObjectPresignRequest.builder()
                                .signatureDuration(ttl)
                                .putObjectRequest(request -> request
                                        .bucket(bucket)
                                        .key(normalizedKey)
                                        .contentType(contentType)
                                        .checksumSHA256(checksumSha256Base64)
                                        .tagging(PENDING_UPLOAD_TAGGING))
                                .build()
                );
                return buildUploadUrl(presignedRequest, contentType, checksumSha256Base64, ttl);
            } catch (SdkException exception) {
                throw new StorageOperationException("File storage is temporarily unavailable", exception);
            }
        });
    }

    @Override
    public PresignedStorageUrl presignDownload(String s3Key) {
        String normalizedKey = s3ObjectKeyValidator.validateAndNormalizeS3Key(s3Key);

        return presignedDownloadUrlCache.get(normalizedKey, this::createPresignedDownloadUrl);
    }

    private PresignedStorageUrl createPresignedDownloadUrl(String s3Key) {
        Duration ttl = storageProperties.s3().downloadPresignTtl();
        String bucket = storageProperties.s3().bucket();

        return recordS3Operation("presign_download", () -> {
            try {
                PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(
                        GetObjectPresignRequest.builder()
                                .signatureDuration(ttl)
                                .getObjectRequest(request -> request
                                        .bucket(bucket)
                                        .key(s3Key)
                                        .responseCacheControl(downloadCacheControl(ttl)))
                                .build()
                );
                return buildDownloadUrl(presignedRequest, ttl);
            } catch (SdkException exception) {
                throw new StorageOperationException("File storage is temporarily unavailable", exception);
            }
        });
    }

    private String downloadCacheControl(Duration ttl) {
        return "public, max-age=" + ttl.toSeconds() + ", immutable";
    }

    @Override
    public StorageObjectMetadata getObjectMetadata(String s3Key) {
        String normalizedKey = s3ObjectKeyValidator.validateAndNormalizeS3Key(s3Key);
        String bucket = storageProperties.s3().bucket();

        return recordS3Operation("head_object", () -> {
            try {
                HeadObjectResponse response = s3Client.headObject(
                        HeadObjectRequest.builder()
                                .bucket(bucket)
                                .key(normalizedKey)
                                .build()
                );
                return buildObjectMetadata(normalizedKey, response);
            } catch (SdkException exception) {
                if (exception instanceof S3Exception s3Exception && s3Exception.statusCode() == S3_NOT_FOUND_STATUS) {
                    throw new ResourceNotFoundException("File object was not found", exception);
                }

                throw new StorageOperationException("File storage is temporarily unavailable", exception);
            }
        });
    }

    @Override
    public void markObjectAsAttached(String s3Key) {
        String normalizedKey = s3ObjectKeyValidator.validateAndNormalizeS3Key(s3Key);
        String bucket = storageProperties.s3().bucket();

        recordS3Operation("put_object_tags", () -> {
            try {
                s3Client.putObjectTagging(
                        PutObjectTaggingRequest.builder()
                                .bucket(bucket)
                                .key(normalizedKey)
                                .tagging(tagging -> tagging.tagSet(List.of(
                                        Tag.builder()
                                                .key(UPLOAD_STATUS_TAG_KEY)
                                                .value(ATTACHED_UPLOAD_STATUS)
                                                .build()
                                )))
                                .build()
                );
            } catch (SdkException exception) {
                throw new StorageOperationException("Failed to update file storage tags", exception);
            }
        });
    }

    @Override
    public boolean isObjectMarkedAsAttached(String s3Key) {
        String normalizedKey = s3ObjectKeyValidator.validateAndNormalizeS3Key(s3Key);

        return recordS3Operation("get_object_tags", () -> {
            try {
                return hasUploadStatusTag(normalizedKey, ATTACHED_UPLOAD_STATUS);
            } catch (SdkException exception) {
                throw new StorageOperationException("Failed to inspect file storage tags", exception);
            }
        });
    }

    @Override
    public List<String> findPendingUploadKeysOlderThan(Instant threshold) {
        if (threshold == null) {
            throw new IllegalArgumentException("Threshold is required");
        }

        return recordS3Operation("find_pending_uploads", () -> {
            String bucket = storageProperties.s3().bucket();
            try {
                List<String> pendingUploadKeys = new ArrayList<>();
                for (String prefix : CLEANUP_PREFIXES) {
                    s3Client.listObjectsV2Paginator(
                                    ListObjectsV2Request.builder()
                                            .bucket(bucket)
                                            .prefix(prefix)
                                            .build()
                            )
                            .contents()
                            .stream()
                            .filter(object -> object.lastModified() != null
                                    && object.lastModified().isBefore(threshold))
                            .map(S3Object::key)
                            .filter(s3Key -> hasUploadStatusTag(s3Key, PENDING_UPLOAD_STATUS))
                            .forEach(pendingUploadKeys::add);
                }

                return List.copyOf(pendingUploadKeys);
            } catch (SdkException exception) {
                throw new StorageOperationException("Failed to inspect pending uploads in storage", exception);
            }
        });
    }

    @Override
    @Retryable(
            retryFor = StorageOperationException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 200, multiplier = 2.0)
    )
    public void deleteObject(String s3Key) {
        String normalizedKey = s3ObjectKeyValidator.validateAndNormalizeS3Key(s3Key);
        recordS3Operation("delete_object", () -> deleteObjectsInBatches(List.of(normalizedKey)));
    }

    @Override
    @Retryable(
            retryFor = StorageOperationException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 200, multiplier = 2.0)
    )
    public void deleteObjects(Collection<String> s3Keys) {
        List<String> normalizedKeys = normalizeKeys(s3Keys);
        if (normalizedKeys.isEmpty()) {
            return;
        }

        recordS3Operation("delete_objects", () -> deleteObjectsInBatches(normalizedKeys));
    }

    private void recordS3Operation(String operation, Runnable action) {
        recordS3Operation(operation, () -> {
            action.run();
            return null;
        });
    }

    private <T> T recordS3Operation(String operation, Supplier<T> action) {
        var sample = storageMetricsRecorder.startTimer();
        String outcome = StorageMetricsRecorder.OUTCOME_FAILURE;
        try {
            T result = action.get();
            outcome = StorageMetricsRecorder.OUTCOME_SUCCESS;
            return result;
        } finally {
            storageMetricsRecorder.recordS3Operation(sample, operation, outcome);
        }
    }

    private List<String> normalizeKeys(Collection<String> s3Keys) {
        if (s3Keys == null || s3Keys.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<String> normalizedKeys = new LinkedHashSet<>();
        s3Keys.forEach(s3Key -> normalizedKeys.add(s3ObjectKeyValidator.validateAndNormalizeS3Key(s3Key)));
        return List.copyOf(normalizedKeys);
    }

    private void deleteObjectsInBatches(List<String> s3Keys) {
        for (int start = 0; start < s3Keys.size(); start += DELETE_OBJECTS_BATCH_SIZE) {
            int end = Math.min(start + DELETE_OBJECTS_BATCH_SIZE, s3Keys.size());
            deleteBatch(s3Keys.subList(start, end));
        }
    }

    private void deleteBatch(List<String> s3Keys) {
        String bucket = storageProperties.s3().bucket();

        try {
            DeleteObjectsResponse response = s3Client.deleteObjects(
                    DeleteObjectsRequest.builder()
                            .bucket(bucket)
                            .delete(delete -> delete
                                    .objects(s3Keys.stream()
                                            .map(s3Key -> ObjectIdentifier.builder().key(s3Key).build())
                                            .toList())
                                    .quiet(true))
                            .build()
            );
            if (response.hasErrors()) {
                throw new StorageOperationException("Failed to delete one or more files from storage");
            }
            presignedDownloadUrlCache.invalidateAll(s3Keys);
        } catch (SdkException exception) {
            throw new StorageOperationException("Failed to delete one or more files from storage", exception);
        }
    }

    private PresignedStorageUrl buildUploadUrl(
            PresignedPutObjectRequest presignedRequest,
            String contentType,
            String checksumSha256Base64,
            Duration ttl
    ) {
        return PresignedStorageUrl.builder()
                .url(presignedRequest.url().toExternalForm())
                .expiresAt(Instant.now(clock).plus(ttl))
                .requiredHeaders(Map.of(
                        CONTENT_TYPE_HEADER, contentType,
                        CHECKSUM_SHA256_HEADER, checksumSha256Base64,
                        TAGGING_HEADER, PENDING_UPLOAD_TAGGING
                ))
                .build();
    }

    private PresignedStorageUrl buildDownloadUrl(PresignedGetObjectRequest presignedRequest, Duration ttl) {
        return PresignedStorageUrl.builder()
                .url(presignedRequest.url().toExternalForm())
                .expiresAt(Instant.now(clock).plus(ttl))
                .requiredHeaders(Map.of())
                .build();
    }

    private StorageObjectMetadata buildObjectMetadata(String s3Key, HeadObjectResponse response) {
        return StorageObjectMetadata.builder()
                .s3Key(s3Key)
                .contentType(response.contentType())
                .sizeBytes(response.contentLength())
                .build();
    }

    private boolean hasUploadStatusTag(String s3Key, String expectedStatus) {
        GetObjectTaggingResponse response = s3Client.getObjectTagging(
                GetObjectTaggingRequest.builder()
                        .bucket(storageProperties.s3().bucket())
                        .key(s3Key)
                        .build()
        );

        return response.tagSet().stream().anyMatch(tag ->
                UPLOAD_STATUS_TAG_KEY.equals(tag.key()) && expectedStatus.equals(tag.value())
        );
    }
}
