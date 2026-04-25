package org.plishka.backend.service.storage.validation;

import java.util.Base64;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.config.properties.StorageProperties;
import org.plishka.backend.domain.media.MediaType;
import org.plishka.backend.dto.file.PresignUploadRequestDto;
import org.plishka.backend.exception.BadRequestException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class FileMetadataValidator {
    private static final int MAX_ORIGINAL_FILENAME_LENGTH = 255;
    private static final int MIN_FILE_SIZE_BYTES = 1;
    private static final int ASCII_CONTROL_CHAR_LIMIT = 32;
    private static final int ASCII_DELETE_CHAR = 127;
    private static final int SHA256_CHECKSUM_BYTES = 32;

    private final StorageProperties storageProperties;
    private final MediaFileTypeRules mediaFileTypeRules;

    public ValidatedFileMetadata validate(PresignUploadRequestDto request) {
        MediaType mediaType = requireMediaType(request.mediaType());
        String contentType = normalizeContentType(request.contentType());
        requireMatchingContentType(mediaType, contentType);
        final String objectKeyExtension = resolveObjectKeyExtension(mediaType, contentType);

        requireAllowedSize(mediaType, request.sizeBytes());
        validateOriginalFilename(request.originalFilename(), mediaType, contentType);

        String checksumSha256Base64 = normalizeChecksumSha256Base64(request.checksumSha256Base64());
        return new ValidatedFileMetadata(contentType, objectKeyExtension, checksumSha256Base64);
    }

    private MediaType requireMediaType(MediaType mediaType) {
        if (mediaType == null) {
            throw new BadRequestException("Media type is required");
        }

        return mediaType;
    }

    private String normalizeContentType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            throw new BadRequestException("Content type is required");
        }

        return contentType.trim().toLowerCase(Locale.ROOT);
    }

    private void requireMatchingContentType(MediaType mediaType, String contentType) {
        MediaType contentMediaType = mediaFileTypeRules.mediaTypeForContentType(contentType)
                .orElseThrow(() -> new BadRequestException("Content type is not allowed"));

        if (mediaType != contentMediaType) {
            throw new BadRequestException("Content type does not match media type");
        }
    }

    private String resolveObjectKeyExtension(MediaType mediaType, String contentType) {
        return mediaFileTypeRules.objectKeyExtensionFor(mediaType, contentType).orElseThrow();
    }

    private void requireAllowedSize(MediaType mediaType, Long sizeBytes) {
        if (sizeBytes == null || sizeBytes < MIN_FILE_SIZE_BYTES) {
            throw new BadRequestException("File size must be greater than 0");
        }

        long maxSizeBytes = switch (mediaType) {
            case IMAGE -> storageProperties.image().maxSize().toBytes();
            case VIDEO -> storageProperties.video().maxSize().toBytes();
        };
        if (sizeBytes > maxSizeBytes) {
            throw new BadRequestException("File size exceeds the allowed limit");
        }
    }

    private void validateOriginalFilename(String originalFilename, MediaType mediaType, String contentType) {
        if (!StringUtils.hasText(originalFilename)) {
            throw new BadRequestException("Original filename is required");
        }

        String filename = originalFilename.trim();
        if (filename.length() > MAX_ORIGINAL_FILENAME_LENGTH) {
            throw new BadRequestException("Original filename must not exceed 255 characters");
        }

        if (filename.contains("/") || filename.contains("\\")) {
            throw new BadRequestException("Original filename must not contain path separators");
        }

        if (filename.contains("..")) {
            throw new BadRequestException("Original filename must not contain path traversal sequences");
        }

        if (filename.chars().anyMatch(this::isControlCharacter)) {
            throw new BadRequestException("Original filename must not contain control characters");
        }

        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex <= 0 || dotIndex == filename.length() - 1) {
            throw new BadRequestException("Original filename must include an extension");
        }

        String basename = filename.substring(0, dotIndex);
        if (basename.contains(".")) {
            throw new BadRequestException("Original filename must contain only one extension");
        }

        String extension = filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        if (!mediaFileTypeRules.allowsOriginalFilenameExtension(mediaType, contentType, extension)) {
            throw new BadRequestException("File extension does not match content type");
        }
    }

    private String normalizeChecksumSha256Base64(String checksumSha256Base64) {
        if (!StringUtils.hasText(checksumSha256Base64)) {
            throw new BadRequestException("Base64 SHA-256 checksum is required");
        }

        byte[] decodedChecksum;
        try {
            decodedChecksum = Base64.getDecoder().decode(checksumSha256Base64.trim());
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Base64 SHA-256 checksum must be a valid Base64 value", exception);
        }

        if (decodedChecksum.length != SHA256_CHECKSUM_BYTES) {
            throw new BadRequestException("Base64 SHA-256 checksum must decode to 32 bytes");
        }

        return Base64.getEncoder().encodeToString(decodedChecksum);
    }

    private boolean isControlCharacter(int character) {
        return character < ASCII_CONTROL_CHAR_LIMIT || character == ASCII_DELETE_CHAR;
    }

    public record ValidatedFileMetadata(
            String contentType,
            String normalizedExtension,
            String checksumSha256Base64
    ) {
    }
}
