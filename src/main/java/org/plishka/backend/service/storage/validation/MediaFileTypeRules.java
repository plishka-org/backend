package org.plishka.backend.service.storage.validation;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.plishka.backend.domain.media.MediaType;
import org.springframework.stereotype.Component;

@Component
public class MediaFileTypeRules {
    private static final List<FileTypeRule> FILE_TYPE_RULES = List.of(
            rule(MediaType.IMAGE, "image/jpeg", "jpg", "jpg", "jpeg"),
            rule(MediaType.IMAGE, "image/png", "png"),
            rule(MediaType.IMAGE, "image/webp", "webp"),
            rule(MediaType.VIDEO, "video/mp4", "mp4"),
            rule(MediaType.VIDEO, "video/webm", "webm")
    );

    public Optional<MediaType> mediaTypeForContentType(String contentType) {
        return ruleForContentType(contentType).map(FileTypeRule::mediaType);
    }

    public Optional<String> objectKeyExtensionFor(MediaType mediaType, String contentType) {
        return ruleFor(mediaType, contentType).map(FileTypeRule::objectKeyExtension);
    }

    public boolean allowsOriginalFilenameExtension(MediaType mediaType, String contentType, String extension) {
        return ruleFor(mediaType, contentType)
                .map(rule -> rule.allowsOriginalFilenameExtension(extension))
                .orElse(false);
    }

    public boolean allowsObjectKeyExtension(MediaType mediaType, String extension) {
        return FILE_TYPE_RULES.stream()
                .filter(rule -> rule.hasMediaType(mediaType))
                .anyMatch(rule -> rule.hasObjectKeyExtension(extension));
    }

    private static FileTypeRule rule(MediaType mediaType, String contentType, String extension) {
        return rule(mediaType, contentType, extension, extension);
    }

    private static FileTypeRule rule(
            MediaType mediaType,
            String contentType,
            String objectKeyExtension,
            String... originalExtensions
    ) {
        return new FileTypeRule(mediaType, contentType, objectKeyExtension, Set.of(originalExtensions));
    }

    private Optional<FileTypeRule> ruleForContentType(String contentType) {
        return FILE_TYPE_RULES.stream()
                .filter(rule -> rule.hasContentType(contentType))
                .findFirst();
    }

    private Optional<FileTypeRule> ruleFor(MediaType mediaType, String contentType) {
        return FILE_TYPE_RULES.stream()
                .filter(rule -> rule.hasMediaType(mediaType))
                .filter(rule -> rule.hasContentType(contentType))
                .findFirst();
    }

    private record FileTypeRule(
            MediaType mediaType,
            String contentType,
            String objectKeyExtension,
            Set<String> originalExtensions
    ) {
        private boolean hasMediaType(MediaType mediaType) {
            return this.mediaType == mediaType;
        }

        private boolean hasContentType(String contentType) {
            return this.contentType.equals(contentType);
        }

        private boolean allowsOriginalFilenameExtension(String extension) {
            return originalExtensions.contains(extension);
        }

        private boolean hasObjectKeyExtension(String extension) {
            return objectKeyExtension.equals(extension);
        }
    }
}
