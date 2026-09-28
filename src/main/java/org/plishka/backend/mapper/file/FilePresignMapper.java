package org.plishka.backend.mapper.file;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.plishka.backend.config.MapStructConfig;
import org.plishka.backend.dto.file.PresignDownloadResponseDto;
import org.plishka.backend.dto.file.PresignUploadResponseDto;
import org.plishka.backend.service.storage.model.PresignedStorageUrl;

@Mapper(config = MapStructConfig.class)
public interface FilePresignMapper {
    @Mapping(target = "s3Key", source = "s3Key")
    @Mapping(target = "uploadUrl", source = "presignedUrl.url")
    @Mapping(target = "method", constant = "PUT")
    @Mapping(target = "expiresAt", source = "presignedUrl.expiresAt")
    @Mapping(target = "requiredHeaders", expression = "java(java.util.Map.copyOf(presignedUrl.requiredHeaders()))")
    PresignUploadResponseDto toUploadResponseDto(String s3Key, PresignedStorageUrl presignedUrl);

    @Mapping(target = "s3Key", source = "s3Key")
    @Mapping(target = "downloadUrl", source = "presignedUrl.url")
    @Mapping(target = "method", constant = "GET")
    @Mapping(target = "expiresAt", source = "presignedUrl.expiresAt")
    PresignDownloadResponseDto toDownloadResponseDto(String s3Key, PresignedStorageUrl presignedUrl);
}
