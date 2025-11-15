package com.twodo0.capstoneWeb.prediction.dto;

public record PredictionJobCreateRequest(
        Long imageId,
        Double yoloThreshold
) {}
