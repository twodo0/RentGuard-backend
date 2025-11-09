package com.twodo0.capstoneWeb.dto;

import com.twodo0.capstoneWeb.domain.enums.RentalStatus;

public record RentalStartRes(
        Long rentalSessionId,
        String vehicleNo,
        Long predictionId,
        ConditionSummary start,
        RentalStatus rentalStatus
) {
}
