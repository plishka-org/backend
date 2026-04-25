package org.plishka.backend.service.file;

import org.plishka.backend.domain.media.MediaTargetType;

public interface MediaReferenceService {
    void assertParentExists(MediaTargetType targetType, Long targetId);

    boolean isAttached(String s3Key);
}
