package org.plishka.backend.service.storage;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.plishka.backend.service.storage.model.PresignedStorageUrl;
import org.plishka.backend.service.storage.model.StorageObjectMetadata;

public interface ObjectStorageService {
    PresignedStorageUrl presignUpload(String s3Key, String contentType, String checksumSha256Base64);

    PresignedStorageUrl presignDownload(String s3Key);

    StorageObjectMetadata getObjectMetadata(String s3Key);

    void markObjectAsAttached(String s3Key);

    List<String> findPendingUploadKeysOlderThan(Instant threshold);

    void deleteObject(String s3Key);

    void deleteObjects(Collection<String> s3Keys);
}
