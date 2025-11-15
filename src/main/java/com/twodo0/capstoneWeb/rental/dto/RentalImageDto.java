package com.twodo0.capstoneWeb.rental.dto;

import com.twodo0.capstoneWeb.prediction.domain.enums.DamageType;
import com.twodo0.capstoneWeb.prediction.dto.DetectionDto;
import com.twodo0.capstoneWeb.rental.domain.enums.ImageSlot;

import java.util.List;
import java.util.Map;

public record RentalImageDto(
        ImageSlot slot,
        Long predictionId,
        String rawUrl,
        String heatmapUrl,
        Map<DamageType, Integer> summaryByImage,
        List<DetectionDto> detections
) {}
