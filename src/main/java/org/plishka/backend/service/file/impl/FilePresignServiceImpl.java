package org.plishka.backend.service.file.impl;

import lombok.RequiredArgsConstructor;
import org.plishka.backend.dto.file.PresignDownloadRequestDto;
import org.plishka.backend.dto.file.PresignDownloadResponseDto;
import org.plishka.backend.dto.file.PresignUploadRequestDto;
import org.plishka.backend.dto.file.PresignUploadResponseDto;
import org.plishka.backend.exception.ResourceNotFoundException;
import org.plishka.backend.mapper.file.FilePresignMapper;
import org.plishka.backend.monitoring.metrics.StorageMetricsRecorder;
import org.plishka.backend.service.file.FilePresignService;
import org.plishka.backend.service.file.MediaReferenceService;
import org.plishka.backend.service.storage.ObjectStorageService;
import org.plishka.backend.service.storage.key.S3ObjectKeyGenerator;
import org.plishka.backend.service.storage.model.PresignedStorageUrl;
import org.plishka.backend.service.storage.validation.FileMetadataValidator;
import org.plishka.backend.service.storage.validation.FileMetadataValidator.ValidatedFileMetadata;
import org.plishka.backend.service.storage.validation.S3ObjectKeyValidator;
import org.plishka.backend.service.storage.validation.S3ObjectKeyValidator.ValidatedS3ObjectKey;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FilePresignServiceImpl implements FilePresignService {
    private final FileMetadataValidator fileMetadataValidator;
    private final S3ObjectKeyGenerator s3ObjectKeyGenerator;
    private final S3ObjectKeyValidator s3ObjectKeyValidator;
    private final ObjectStorageService objectStorageService;
    private final MediaReferenceService mediaReferenceService;
    private final FilePresignMapper filePresignMapper;
    private final StorageMetricsRecorder storageMetricsRecorder;

    @Override
    public PresignUploadResponseDto presignUpload(PresignUploadRequestDto request) {
        var sample = storageMetricsRecorder.startTimer();
        String outcome = StorageMetricsRecorder.OUTCOME_FAILURE;
        try {
            ValidatedFileMetadata metadata = fileMetadataValidator.validate(request);
            requireParentExists(request);

            String s3Key = generateS3Key(request, metadata);
            PresignedStorageUrl presignedUrl = objectStorageService.presignUpload(
                    s3Key,
                    metadata.contentType(),
                    metadata.checksumSha256Base64()
            );
            outcome = StorageMetricsRecorder.OUTCOME_SUCCESS;
            return filePresignMapper.toUploadResponseDto(s3Key, presignedUrl);
        } finally {
            storageMetricsRecorder.recordPresign(
                    sample,
                    "upload",
                    request == null ? null : request.targetType(),
                    request == null ? null : request.mediaType(),
                    outcome
            );
        }
    }

    @Override
    public PresignDownloadResponseDto presignDownload(PresignDownloadRequestDto request) {
        var sample = storageMetricsRecorder.startTimer();
        String outcome = StorageMetricsRecorder.OUTCOME_FAILURE;
        ValidatedS3ObjectKey validatedKey = null;
        try {
            validatedKey = s3ObjectKeyValidator.validateAndParseS3Key(request.s3Key());
            requireAttachedKey(validatedKey.value());

            PresignedStorageUrl presignedUrl = objectStorageService.presignDownload(validatedKey.value());
            outcome = StorageMetricsRecorder.OUTCOME_SUCCESS;
            return filePresignMapper.toDownloadResponseDto(validatedKey.value(), presignedUrl);
        } finally {
            storageMetricsRecorder.recordPresign(
                    sample,
                    "download",
                    validatedKey == null ? null : validatedKey.targetType(),
                    validatedKey == null ? null : validatedKey.mediaType(),
                    outcome
            );
        }
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

    private void requireAttachedKey(String s3Key) {
        if (!mediaReferenceService.isAttached(s3Key)) {
            throw new ResourceNotFoundException("Media file was not found");
        }
    }
}
