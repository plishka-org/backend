package org.plishka.backend.mapper.review;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.plishka.backend.config.MapStructConfig;
import org.plishka.backend.domain.review.Review;
import org.plishka.backend.domain.review.ReviewMedia;
import org.plishka.backend.dto.admin.review.AdminReviewDetailDto;
import org.plishka.backend.dto.admin.review.AdminReviewSummaryDto;
import org.plishka.backend.dto.review.ReviewDetailDto;
import org.plishka.backend.dto.review.ReviewMediaDto;
import org.plishka.backend.dto.review.ReviewMediaPreviewDto;
import org.plishka.backend.dto.review.ReviewSummaryDto;

@Mapper(config = MapStructConfig.class)
public interface ReviewMapper {
    @Mapping(target = "reviewId", source = "review.id")
    @Mapping(target = "media", source = "media")
    ReviewDetailDto toDetailDto(Review review, List<ReviewMedia> media);

    @Mapping(target = "reviewId", source = "review.id")
    @Mapping(target = "authorName", source = "review.authorName")
    @Mapping(target = "content", source = "review.content")
    @Mapping(target = "createdAt", source = "review.createdAt")
    @Mapping(target = "primaryMedia", source = "primaryMedia")
    ReviewSummaryDto toSummaryDto(Review review, ReviewMedia primaryMedia);

    @Mapping(target = "reviewId", source = "review.id")
    @Mapping(target = "isFeatured", source = "review.isFeatured")
    @Mapping(target = "media", source = "media")
    AdminReviewDetailDto toAdminDetailDto(Review review, List<ReviewMedia> media);

    @Mapping(target = "reviewId", source = "review.id")
    @Mapping(target = "authorName", source = "review.authorName")
    @Mapping(target = "content", source = "review.content")
    @Mapping(target = "createdAt", source = "review.createdAt")
    @Mapping(target = "isFeatured", source = "review.isFeatured")
    @Mapping(target = "primaryMedia", source = "primaryMedia")
    AdminReviewSummaryDto toAdminSummaryDto(Review review, ReviewMedia primaryMedia);

    @Mapping(target = "reviewMediaId", source = "id")
    ReviewMediaDto toMediaDto(ReviewMedia media);

    @Mapping(target = "reviewMediaId", source = "id")
    ReviewMediaPreviewDto toMediaPreviewDto(ReviewMedia media);
}
