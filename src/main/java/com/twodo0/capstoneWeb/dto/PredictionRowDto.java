package com.twodo0.capstoneWeb.dto;

import java.time.OffsetDateTime;

public record PredictionRowDto(
        Long predictionId,
        OffsetDateTime createdAt,
        String imageBucket,
        String imageKey,
        String previewBucket,
        String previewKey,
        Long detectionCount
) {
}