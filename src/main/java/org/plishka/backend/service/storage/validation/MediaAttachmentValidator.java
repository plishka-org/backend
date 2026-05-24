package org.plishka.backend.service.storage.validation;

import lombok.RequiredArgsConstructor;
import org.plishka.backend.domain.media.MediaTargetType;
import org.plishka.backend.domain.media.MediaType;
import org.plishka.backend.exception.BadRequestException;
import org.plishka.backend.service.storage.ObjectStorageService;
import org.plishka.backend.service.storage.model.StorageObjectMetadata;
import org.plishka.backend.service.storage.validation.S3ObjectKeyValidator.ValidatedS3ObjectKey;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MediaAttachmentValidator {
    private final ObjectStorageService objectStorageService;
    private final MediaFileTypeRules mediaFileTypeRules;
    private final S3ObjectKeyValidator s3ObjectKeyValidator;

    public ValidatedMediaAttachment validate(
            String s3Key,
            MediaTargetType expectedTargetType,
            Long expectedTargetId,
            String targetName
    ) {
        ValidatedS3ObjectKey validatedKey = s3ObjectKeyValidator.validateAndParseS3Key(s3Key);

        if (validatedKey.targetType() != expectedTargetType || !validatedKey.targetId().equals(expectedTargetId)) {
            throw new BadRequestException("S3 key does not match " + targetName + " target");
        }

        MediaType mediaType = resolveAndValidateMediaType(validatedKey);

        return new ValidatedMediaAttachment(validatedKey.value(), mediaType);
    }

    private MediaType resolveAndValidateMediaType(ValidatedS3ObjectKey s3ObjectKey) {
        StorageObjectMetadata metadata = objectStorageService.getObjectMetadata(s3ObjectKey.value());
        MediaType mediaType = mediaFileTypeRules.mediaTypeForContentType(metadata.contentType())
                .orElseThrow(() -> new BadRequestException("Unsupported media content type from S3"));

        if (mediaType != s3ObjectKey.mediaType()) {
            throw new BadRequestException("S3 key media type does not match file content type");
        }

        return mediaType;
    }

    public record ValidatedMediaAttachment(String s3Key, MediaType mediaType) {
    }
}
