package com.twodo0.capstoneWeb.prediction.mapper;

import com.twodo0.capstoneWeb.prediction.domain.PredictionJob;
import com.twodo0.capstoneWeb.prediction.dto.PredictionJobDto;

public class ToResponseDto {
    public static PredictionJobDto toDto(PredictionJob job){
        return PredictionJobDto.builder()
                .jobId(job.getId())
                .predictionId(job.getPredictionId())
                .status(job.getStatus())
                .errorMessage(job.getErrorMessage())
                .createdAt(job.getCreatedAt())
                .startedAt(job.getStartedAt())
                .finishedAt(job.getFinishedAt())
                .build();
    }
}
