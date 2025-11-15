package com.twodo0.capstoneWeb.prediction.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    private String heatmapUrl;
    @Builder.Default
    private List<DetectionDto> detections = List.of();
    private Integer width; //원본 너비
    private Integer height; // 원본 높이
}