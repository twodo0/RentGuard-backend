package com.twodo0.capstoneWeb.dto;

import com.twodo0.capstoneWeb.domain.enums.DamageType;
import com.twodo0.capstoneWeb.domain.enums.RentalStatus;

import java.util.Map;

public record RentalFinishRes(
        Long rentalSessionId,
        String vehicleNo,
        Long predictionId,
        ConditionSummary finish,
        Map<DamageType, Integer> delta,
        RentalStatus rentalStatus
) {
}
