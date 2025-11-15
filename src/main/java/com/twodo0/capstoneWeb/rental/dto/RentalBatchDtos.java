package com.twodo0.capstoneWeb.rental.dto;

import com.twodo0.capstoneWeb.prediction.domain.enums.DamageType;
import com.twodo0.capstoneWeb.rental.domain.enums.ImageSlot;
import com.twodo0.capstoneWeb.rental.domain.enums.RentalStatus;

import java.util.List;
import java.util.Map;

public final class RentalBatchDtos {
    // 한 장 단위
    public static record ImageItem(Long imageId, ImageSlot slot){}
    public static record ImageResult(ImageSlot slot, Long predictionId){}

    // 시작 배치 요청
    public record RentalStartBatchReq(
            String vehicleNo,
            List<ImageItem> images,
            Double yoloThreshold
    ){}

    //시작 배치 응답
    public record RentalStartBatchRes(
            Long rentalId,
            String vehicleNo,
            Integer totalDamage,
            Map<DamageType, Integer> startSummary, // 세션 전체 합산
            List<ImageResult> results, // 이미지별 predictionId
            RentalStatus status // IN_RENT
    ){}

    public record RentalFinishBatchReq(
            Long rentalId,
            String vehicleNo,
            List<ImageItem> images,
            Double yoloThreshold
    ){}

    public record RentalFinishBatchRes(
            Long rentalId,
            String vehicleNo,
            Integer totalDamage,
            Map<DamageType, Integer> finishSummary,
            Map<DamageType, Integer> delta,
            List<ImageResult> results,
            RentalStatus status
    ){}
}
