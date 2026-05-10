package org.plishka.backend.dto.about;

public record AboutPageContentDto(
        String historyTitle,
        String historyText,
        String currentTitle,
        String currentText
) {
}
