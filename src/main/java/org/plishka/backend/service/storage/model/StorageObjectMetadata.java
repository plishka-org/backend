package org.plishka.backend.service.storage.model;

import lombok.Builder;

@Builder
public record StorageObjectMetadata(
        String s3Key,
        String contentType,
        long sizeBytes
) {
}
