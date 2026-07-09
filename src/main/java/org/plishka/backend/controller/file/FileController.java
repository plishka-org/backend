package org.plishka.backend.controller.file;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.dto.file.PresignDownloadRequestDto;
import org.plishka.backend.dto.file.PresignDownloadResponseDto;
import org.plishka.backend.openapi.support.OpenApiExampleValues;
import org.plishka.backend.service.file.FilePresignService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
@Tag(name = "Files")
public class FileController {
    private final FilePresignService filePresignService;

    @Operation(
            operationId = "presignDownload",
            summary = "Create presigned download URL",
            description = "Creates a temporary download URL for an attached media object. "
                    + "The S3 key is an opaque string for clients."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(examples = @ExampleObject(value = OpenApiExampleValues.PRESIGN_DOWNLOAD_REQUEST_JSON))
    )
    @PostMapping("/presign/download")
    public PresignDownloadResponseDto presignDownload(@Valid @RequestBody PresignDownloadRequestDto requestDto) {
        return filePresignService.presignDownload(requestDto);
    }
}
