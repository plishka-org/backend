package org.plishka.backend.controller.file;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.dto.file.PresignDownloadRequestDto;
import org.plishka.backend.dto.file.PresignDownloadResponseDto;
import org.plishka.backend.dto.file.PresignUploadRequestDto;
import org.plishka.backend.dto.file.PresignUploadResponseDto;
import org.plishka.backend.service.file.FilePresignService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {
    private final FilePresignService filePresignService;

    @PostMapping("/presign/upload")
    public PresignUploadResponseDto presignUpload(@Valid @RequestBody PresignUploadRequestDto requestDto) {
        return filePresignService.presignUpload(requestDto);
    }

    @PostMapping("/presign/download")
    public PresignDownloadResponseDto presignDownload(@Valid @RequestBody PresignDownloadRequestDto requestDto) {
        return filePresignService.presignDownload(requestDto);
    }
}
