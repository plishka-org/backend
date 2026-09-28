package org.plishka.backend.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMax;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "storage")
public record StorageProperties(
        @NotNull(message = "storage.s3 must not be null")
        @Valid
        S3 s3,

        @NotNull(message = "storage.image must not be null")
        @Valid
        MediaConstraints image,

        @NotNull(message = "storage.video must not be null")
        @Valid
        MediaConstraints video,

        @NotNull(message = "storage.cleanup must not be null")
        @Valid
        Cleanup cleanup
) {
    public record S3(
            @NotBlank(message = "storage.s3.bucket must not be blank")
            String bucket,

            @NotBlank(message = "storage.s3.region must not be blank")
            String region,

            @NotBlank(message = "storage.s3.access-key must not be blank")
            String accessKey,

            @NotBlank(message = "storage.s3.secret-key must not be blank")
            String secretKey,

            String endpointUrl,

            @NotNull(message = "storage.s3.upload-presign-ttl must not be null")
            @DurationMin(nanos = 1, message = "storage.s3.upload-presign-ttl must be greater than 0")
            @DurationMax(days = 7, message = "storage.s3.upload-presign-ttl must not exceed 7 days")
            Duration uploadPresignTtl,

            @NotNull(message = "storage.s3.download-presign-ttl must not be null")
            @DurationMin(nanos = 1, message = "storage.s3.download-presign-ttl must be greater than 0")
            @DurationMax(days = 7, message = "storage.s3.download-presign-ttl must not exceed 7 days")
            Duration downloadPresignTtl
    ) {
        @AssertTrue(message = "storage.s3.endpoint-url must be a valid URL when provided")
        public boolean isEndpointUrlValidWhenProvided() {
            if (!StringUtils.hasText(endpointUrl)) {
                return true;
            }

            try {
                URI endpointUri = URI.create(endpointUrl);
                return endpointUri.isAbsolute() && StringUtils.hasText(endpointUri.getHost());
            } catch (IllegalArgumentException exception) {
                return false;
            }
        }
    }

    public record MediaConstraints(
            @NotNull(message = "storage media max-size must not be null")
            DataSize maxSize
    ) {
        @AssertTrue(message = "storage media max-size must be greater than 0")
        public boolean isMaxSizePositive() {
            return maxSize == null || maxSize.toBytes() > 0;
        }
    }

    public record Cleanup(
            boolean enabled,

            @DurationMin(nanos = 1, message = "storage.cleanup.orphan-upload-ttl must be greater than 0")
            Duration orphanUploadTtl
    ) {
        @AssertTrue(message = "storage.cleanup.orphan-upload-ttl must not be null when storage.cleanup.enabled is true")
        public boolean isOrphanUploadTtlPresentWhenEnabled() {
            if (!enabled) {
                return true;
            }
            return orphanUploadTtl != null;
        }
    }
}
