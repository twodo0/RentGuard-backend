package com.twodo0.capstoneWeb.rental.dto;

import com.twodo0.capstoneWeb.rental.domain.enums.RentalStatus;

import java.time.OffsetDateTime;

public record RentalRowView(
        Long rentalId,
        String vehicleNo,
        RentalStatus status,
        OffsetDateTime startAt,
        OffsetDateTime finishedAt,
        Integer newDamageTotal
) {
}
