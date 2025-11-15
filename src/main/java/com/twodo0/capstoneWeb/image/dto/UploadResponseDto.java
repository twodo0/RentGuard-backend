package com.twodo0.capstoneWeb.image.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UploadResponseDto {
    private Long imageId;
    private String contentType;
    private Integer width;
    private Integer height;
    private String rawUrl;
}
