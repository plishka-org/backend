package org.plishka.backend.service.storage.impl;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.plishka.backend.config.properties.StorageProperties;
import org.plishka.backend.monitoring.metrics.StorageMetricsRecorder;
import org.plishka.backend.service.storage.PresignedDownloadUrlCache;
import org.plishka.backend.service.storage.model.PresignedStorageUrl;
import org.plishka.backend.service.storage.validation.S3ObjectKeyValidator;
import org.springframework.util.unit.DataSize;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3ObjectStorageServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-09T12:00:00Z");
    private static final Duration DOWNLOAD_TTL = Duration.ofHours(2);
    private static final String BUCKET = "test-bucket";
    private static final String S3_KEY =
            "products/1/images/2026/08/123e4567-e89b-12d3-a456-426614174000.jpg";
    private static final String CACHE_CONTROL = "public, max-age=" + DOWNLOAD_TTL.toSeconds() + ", immutable";

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private S3ObjectKeyValidator s3ObjectKeyValidator;

    private S3ObjectStorageService service;

    @BeforeEach
    void setUp() {
        when(s3ObjectKeyValidator.validateAndNormalizeS3Key(S3_KEY)).thenReturn(S3_KEY);

        service = new S3ObjectStorageService(
                s3Client,
                s3Presigner,
                storageProperties(),
                s3ObjectKeyValidator,
                Clock.fixed(NOW, ZoneOffset.UTC),
                new StorageMetricsRecorder(new SimpleMeterRegistry()),
                new PresignedDownloadUrlCache(DOWNLOAD_TTL)
        );
    }

    @Test
    void presignDownload_ShouldSetCacheControlAndReuseGeneratedUrl() throws Exception {
        PresignedGetObjectRequest signedRequest = signedGetRequest("first");
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(signedRequest);

        PresignedStorageUrl first = service.presignDownload(S3_KEY);
        PresignedStorageUrl cached = service.presignDownload(S3_KEY);

        assertSame(first, cached);
        assertEquals(NOW.plus(DOWNLOAD_TTL), first.expiresAt());

        ArgumentCaptor<GetObjectPresignRequest> requestCaptor =
                ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        verify(s3Presigner).presignGetObject(requestCaptor.capture());

        GetObjectPresignRequest request = requestCaptor.getValue();
        assertEquals(DOWNLOAD_TTL, request.signatureDuration());
        assertEquals(BUCKET, request.getObjectRequest().bucket());
        assertEquals(S3_KEY, request.getObjectRequest().key());
        assertEquals(CACHE_CONTROL, request.getObjectRequest().responseCacheControl());
    }

    @Test
    void deleteObject_ShouldInvalidateCachedDownloadUrl() throws Exception {
        PresignedGetObjectRequest firstSignedRequest = signedGetRequest("first");
        PresignedGetObjectRequest secondSignedRequest = signedGetRequest("second");
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenReturn(firstSignedRequest, secondSignedRequest);
        when(s3Client.deleteObjects(any(DeleteObjectsRequest.class)))
                .thenReturn(DeleteObjectsResponse.builder().build());

        service.presignDownload(S3_KEY);
        service.deleteObject(S3_KEY);
        service.presignDownload(S3_KEY);

        verify(s3Presigner, times(2)).presignGetObject(any(GetObjectPresignRequest.class));
    }

    private static PresignedGetObjectRequest signedGetRequest(String signature) throws Exception {
        PresignedGetObjectRequest request = mock(PresignedGetObjectRequest.class);
        when(request.url()).thenReturn(URI.create(
                "https://storage.example.com/image?signature=" + signature
        ).toURL());
        return request;
    }

    private static StorageProperties storageProperties() {
        return new StorageProperties(
                new StorageProperties.S3(
                        BUCKET,
                        "eu-central-1",
                        "test-access-key",
                        "test-secret-key",
                        null,
                        Duration.ofMinutes(15),
                        DOWNLOAD_TTL
                ),
                new StorageProperties.MediaConstraints(DataSize.ofMegabytes(10)),
                new StorageProperties.MediaConstraints(DataSize.ofMegabytes(200)),
                new StorageProperties.Cleanup(false, null)
        );
    }
}
