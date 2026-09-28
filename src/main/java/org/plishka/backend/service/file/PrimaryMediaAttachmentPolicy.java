package org.plishka.backend.service.file;

import org.plishka.backend.domain.media.MediaType;
import org.plishka.backend.exception.BadRequestException;
import org.springframework.stereotype.Component;

@Component
public class PrimaryMediaAttachmentPolicy {
    public boolean shouldMarkAsPrimary(MediaType mediaType, boolean hasPrimaryMedia) {
        if (hasPrimaryMedia) {
            return false;
        }

        requireFirstPrimaryMediaIsImage(mediaType);

        return true;
    }

    private void requireFirstPrimaryMediaIsImage(MediaType mediaType) {
        if (mediaType != MediaType.IMAGE) {
            throw new BadRequestException("The first primary media must be an image.");
        }
    }
}
