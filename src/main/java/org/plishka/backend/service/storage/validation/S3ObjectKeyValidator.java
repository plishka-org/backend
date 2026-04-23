package org.plishka.backend.service.storage.validation;

import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.domain.media.MediaTargetType;
import org.plishka.backend.domain.media.MediaType;
import org.plishka.backend.exception.BadRequestException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class S3ObjectKeyValidator {
    private static final Pattern POSITIVE_LONG_PATTERN = Pattern.compile("[1-9]\\d*");
    private static final Pattern YEAR_PATTERN = Pattern.compile("\\d{4}");
    private static final Pattern MONTH_PATTERN = Pattern.compile("0[1-9]|1[0-2]");
    private static final Pattern CANONICAL_UUID_PATTERN = Pattern.compile(
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    private static final String INVALID_KEY_FORMAT_MESSAGE = "S3 key has invalid format";

    private static final int TARGET_TYPE_PART = 0;
    private static final int TARGET_ID_PART = 1;
    private static final int MEDIA_TYPE_PART = 2;
    private static final int YEAR_PART = 3;
    private static final int MONTH_PART = 4;
    private static final int FILENAME_PART = 5;
    private static final int KEY_PARTS_COUNT = 6;

    private final MediaFileTypeRules mediaFileTypeRules;

    public String validateAndNormalizeS3Key(String s3Key) {
        String normalizedKey = normalizeS3Key(s3Key);
        String[] keyParts = splitKey(normalizedKey);

        requireTargetType(keyParts[TARGET_TYPE_PART]);
        requireTargetId(keyParts[TARGET_ID_PART]);
        MediaType mediaType = resolveMediaType(keyParts[MEDIA_TYPE_PART]);
        requireYear(keyParts[YEAR_PART]);
        requireMonth(keyParts[MONTH_PART]);
        validateFilename(keyParts[FILENAME_PART], mediaType);

        return normalizedKey;
    }

    private String normalizeS3Key(String s3Key) {
        if (!StringUtils.hasText(s3Key)) {
            throw new BadRequestException("S3 key is required");
        }

        return s3Key.trim();
    }

    private String[] splitKey(String s3Key) {
        String[] parts = s3Key.split("/", -1);
        if (parts.length != KEY_PARTS_COUNT) {
            throw new BadRequestException(INVALID_KEY_FORMAT_MESSAGE);
        }

        return parts;
    }

    private void requireTargetType(String keySegment) {
        if (MediaTargetType.fromKeySegment(keySegment).isEmpty()) {
            throw new BadRequestException(INVALID_KEY_FORMAT_MESSAGE);
        }
    }

    private void requireTargetId(String targetId) {
        requireMatches(POSITIVE_LONG_PATTERN, targetId);
    }

    private MediaType resolveMediaType(String keyFolder) {
        return MediaType.fromKeyFolder(keyFolder)
                .orElseThrow(() -> new BadRequestException(INVALID_KEY_FORMAT_MESSAGE));
    }

    private void requireYear(String year) {
        requireMatches(YEAR_PATTERN, year);
    }

    private void requireMonth(String month) {
        requireMatches(MONTH_PATTERN, month);
    }

    private void validateFilename(String filename, MediaType mediaType) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex <= 0 || dotIndex == filename.length() - 1) {
            throw new BadRequestException(INVALID_KEY_FORMAT_MESSAGE);
        }

        String uuid = filename.substring(0, dotIndex);
        requireMatches(CANONICAL_UUID_PATTERN, uuid);

        String extension = filename.substring(dotIndex + 1);
        if (!mediaFileTypeRules.allowsObjectKeyExtension(mediaType, extension)) {
            throw new BadRequestException("S3 key extension is invalid for media type");
        }
    }

    private void requireMatches(Pattern pattern, String value) {
        if (!pattern.matcher(value).matches()) {
            throw new BadRequestException(INVALID_KEY_FORMAT_MESSAGE);
        }
    }
}
