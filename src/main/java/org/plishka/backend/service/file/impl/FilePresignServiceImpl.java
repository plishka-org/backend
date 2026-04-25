package org.plishka.backend.service.file.impl;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.dto.file.PresignDownloadRequestDto;
import org.plishka.backend.dto.file.PresignDownloadResponseDto;
import org.plishka.backend.dto.file.PresignUploadRequestDto;
import org.plishka.backend.dto.file.PresignUploadResponseDto;
import org.plishka.backend.exception.ResourceNotFoundException;
import org.plishka.backend.service.file.FilePresignService;
import org.plishka.backend.service.file.MediaReferenceService;
import org.plishka.backend.service.storage.ObjectStorageService;
import org.plishka.backend.service.storage.key.S3ObjectKeyGenerator;
import org.plishka.backend.service.storage.model.PresignedStorageUrl;
import org.plishka.backend.service.storage.validation.FileMetadataValidator;
import org.plishka.backend.service.storage.validation.FileMetadataValidator.ValidatedFileMetadata;
import org.plishka.backend.service.storage.validation.S3ObjectKeyValidator;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FilePresignServiceImpl implements FilePresignService {
    private static final String PUT_METHOD = "PUT";
    private static final String GET_METHOD = "GET";

    private final FileMetadataValidator fileMetadataValidator;
    private final S3ObjectKeyGenerator s3ObjectKeyGenerator;
    private final S3ObjectKeyValidator s3ObjectKeyValidator;
    private final ObjectStorageService objectStorageService;
    private final MediaReferenceService mediaReferenceService;

    @Override
    public PresignUploadResponseDto presignUpload(PresignUploadRequestDto request) {
        ValidatedFileMetadata metadata = fileMetadataValidator.validate(request);
        requireParentExists(request);

        String s3Key = generateS3Key(request, metadata);
        PresignedStorageUrl presignedUrl = objectStorageService.presignUpload(
                s3Key,
                metadata.contentType(),
                metadata.checksumSha256Base64()
        );
        return buildUploadResponseDto(s3Key, presignedUrl);
    }

    @Override
    public PresignDownloadResponseDto presignDownload(PresignDownloadRequestDto request) {
        String s3Key = s3ObjectKeyValidator.validateAndNormalizeS3Key(request.s3Key());
        requireAttachedKey(s3Key);

        PresignedStorageUrl presignedUrl = objectStorageService.presignDownload(s3Key);
        return buildDownloadResponseDto(s3Key, presignedUrl);
    }

    private void requireParentExists(PresignUploadRequestDto request) {
        mediaReferenceService.assertParentExists(request.targetType(), request.targetId());
    }

    private String generateS3Key(PresignUploadRequestDto request, ValidatedFileMetadata metadata) {
        return s3ObjectKeyGenerator.generate(
                request.targetType(),
                request.targetId(),
                request.mediaType(),
                metadata.normalizedExtension()
        );
    }

    private PresignUploadResponseDto buildUploadResponseDto(String s3Key, PresignedStorageUrl presignedUrl) {
        return PresignUploadResponseDto.builder()
                .s3Key(s3Key)
                .uploadUrl(presignedUrl.url())
                .method(PUT_METHOD)
                .expiresAt(presignedUrl.expiresAt())
                .requiredHeaders(Map.copyOf(presignedUrl.requiredHeaders()))
                .build();
    }

    private void requireAttachedKey(String s3Key) {
        if (!mediaReferenceService.isAttached(s3Key)) {
            throw new ResourceNotFoundException("Media file was not found");
        }
    }

    private PresignDownloadResponseDto buildDownloadResponseDto(String s3Key, PresignedStorageUrl presignedUrl) {
        return PresignDownloadResponseDto.builder()
                .s3Key(s3Key)
                .downloadUrl(presignedUrl.url())
                .method(GET_METHOD)
                .expiresAt(presignedUrl.expiresAt())
                .build();
    }
}
