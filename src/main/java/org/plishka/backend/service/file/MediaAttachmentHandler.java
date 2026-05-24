package org.plishka.backend.service.file;

import org.plishka.backend.domain.media.MediaTargetType;
import org.plishka.backend.domain.media.MediaType;

public interface MediaAttachmentHandler {
    MediaTargetType targetType();

    String targetName();

    default String targetDescription(Long targetId) {
        return targetName() + " " + targetId;
    }

    boolean existsByS3Key(String s3Key);

    void attachValidatedMedia(Long targetId, String s3Key, MediaType mediaType);
}
