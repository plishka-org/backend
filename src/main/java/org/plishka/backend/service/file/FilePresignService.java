package org.plishka.backend.service.file;

import org.plishka.backend.dto.file.PresignDownloadRequestDto;
import org.plishka.backend.dto.file.PresignDownloadResponseDto;
import org.plishka.backend.dto.file.PresignUploadRequestDto;
import org.plishka.backend.dto.file.PresignUploadResponseDto;

public interface FilePresignService {
    PresignUploadResponseDto presignUpload(PresignUploadRequestDto requestDto);

    PresignDownloadResponseDto presignDownload(PresignDownloadRequestDto requestDto);
}
