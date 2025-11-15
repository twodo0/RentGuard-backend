package com.twodo0.capstoneWeb.prediction.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.twodo0.capstoneWeb.prediction.domain.enums.Status;
import lombok.Builder;

import java.time.OffsetDateTime;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Builder
@JsonInclude(NON_NULL)
public record PredictionJobDto(
        Long jobId,
        Status status,
        Long predictionId,
        String errorMessage,
        OffsetDateTime createdAt,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt
) {
}
