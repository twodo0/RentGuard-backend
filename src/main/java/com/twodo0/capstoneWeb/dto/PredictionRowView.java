package com.twodo0.capstoneWeb.dto;

import java.time.OffsetDateTime;

public record PredictionRowView(
        Long predictionId,
        OffsetDateTime createdAt,
        String imageUrl,
        Long detectionCount
) {
}
