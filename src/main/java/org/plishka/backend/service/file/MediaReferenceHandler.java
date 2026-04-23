package org.plishka.backend.service.file;

import org.plishka.backend.domain.media.MediaTargetType;

public interface MediaReferenceHandler {
    MediaTargetType targetType();

    void assertParentExists(Long targetId);

    boolean existsByS3Key(String s3Key);
}
