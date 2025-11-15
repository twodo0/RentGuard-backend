package com.twodo0.capstoneWeb.rental.dto;

import com.twodo0.capstoneWeb.prediction.domain.enums.DamageType;
import com.twodo0.capstoneWeb.rental.domain.enums.RentalStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record RentalDetailDto(
        Long rentalId,
        String vehicleNo,
        RentalStatus status,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,

        // 세션 전체 요역
        Map<DamageType, Integer> startSummary,
        Map<DamageType, Integer> finishSummary,
        Map<DamageType, Integer> deltaSummary,

        Integer startTotal,
        Integer finishTotal,
        Integer newDamageTotal,

        List<RentalImageDto> startImages,
        List<RentalImageDto> finishImages
) {
}
