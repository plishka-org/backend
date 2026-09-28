package org.plishka.backend.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.dto.file.PresignUploadRequestDto;
import org.plishka.backend.dto.file.PresignUploadResponseDto;
import org.plishka.backend.openapi.support.OpenApiExampleValues;
import org.plishka.backend.service.file.FilePresignService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/files")
@RequiredArgsConstructor
@Tag(name = "Admin - Files")
@SecurityRequirement(name = "bearerAuth")
public class AdminFileController {
    private final FilePresignService filePresignService;

    @Operation(
            operationId = "adminPresignUpload",
            summary = "Admin create presigned upload URL",
            description = "Requires active user with ROLE_ADMIN. Allowed content types: image/jpeg, image/png, "
                    + "image/webp, video/mp4, video/webm. Maximum file sizes are configured by server. "
                    + "S3 keys are opaque strings for clients."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(examples = @ExampleObject(value = OpenApiExampleValues.PRESIGN_UPLOAD_REQUEST_JSON))
    )
    @PostMapping("/presign/upload")
    public PresignUploadResponseDto presignUpload(@Valid @RequestBody PresignUploadRequestDto requestDto) {
        return filePresignService.presignUpload(requestDto);
    }
}
