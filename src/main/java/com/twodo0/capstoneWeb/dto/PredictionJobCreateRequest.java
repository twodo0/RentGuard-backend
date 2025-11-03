package com.twodo0.capstoneWeb.dto;

public record PredictionJobCreateRequest(
        Long imageId,
        Double threshold,
        String model
) {}
