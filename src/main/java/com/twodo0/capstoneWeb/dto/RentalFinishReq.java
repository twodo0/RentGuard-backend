package com.twodo0.capstoneWeb.dto;

public record RentalFinishReq(
        Long imageId,
        Long rentalId,
        Double yoloThreshold,
        Double vitThreshold,
        String model,
        String vehicleNo
) {
}
