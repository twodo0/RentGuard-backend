package com.twodo0.capstoneWeb.controller;

import com.twodo0.capstoneWeb.dto.RentalFinishReq;
import com.twodo0.capstoneWeb.dto.RentalFinishRes;
import com.twodo0.capstoneWeb.dto.RentalReq;
import com.twodo0.capstoneWeb.dto.RentalStartRes;
import com.twodo0.capstoneWeb.service.RentalService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

// 이전에 ImageContorller에서 이미지 업로드 후 imageId 받아서 바로 오케스트레이션

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rentals")
public class RentalController {

    private final RentalService rentalService;

    @PostMapping("/start/upload")
    public RentalStartRes startRental(
            @RequestParam Long imageId,
            @RequestParam String vehicleNo,
            @RequestParam(defaultValue = "0.2") Double yoloThreshold,
            @RequestParam(defaultValue = "0.65") Double vitThreshold,
            @RequestParam(defaultValue = "YOLO - ViT") String model
            )
    {
        RentalReq req = new RentalReq(imageId, vehicleNo, yoloThreshold, vitThreshold, model);
        return rentalService.startRental(req);
    }

    @PostMapping("/end/upload")
    public RentalFinishRes finishRental(
            @RequestParam Long imageId,
            @RequestParam Long rentalId,
            @RequestParam(defaultValue = "0.2") Double yoloThreshold,
            @RequestParam(defaultValue = "0.65") Double vitThreshold,
            @RequestParam(defaultValue = "YOLO - ViT") String model, // 임계값, 모델 -> 안 보내면 start 때 사용한 거 사용
            @RequestParam(required = false) String vehicleNo // 검증용
    ){
        RentalFinishReq finishReq = new RentalFinishReq(imageId, rentalId, yoloThreshold, vitThreshold, model, vehicleNo);
        return rentalService.finishRental(finishReq);
    }

}
