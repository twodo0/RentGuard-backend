package com.twodo0.capstoneWeb.dto;

public record RentalReq(
        Long imageId,
        String vehicleNo,
        double yoloThreshold,
        double vitThreshold,
        String model
) {
}
