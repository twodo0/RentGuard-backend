package com.twodo0.capstoneWeb.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Builder
public class PredictionDetailDto{
    private Long predictionId;
    private OffsetDateTime createdAt;
    private String rawUrl;
    private String heatMapUrl;
    @Builder.Default
    private List<DetectionDto> detections = List.of();
    private Integer width; //원본 너비
    private Integer height; // 원본 높이
}