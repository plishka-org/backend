package org.plishka.backend.service.file;

import org.plishka.backend.domain.media.MediaTargetType;

public interface MediaAttachmentService {
    void attachMedia(MediaTargetType targetType, Long targetId, String s3Key);
}
