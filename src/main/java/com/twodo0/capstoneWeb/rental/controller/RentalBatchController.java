package com.twodo0.capstoneWeb.rental.controller;

import com.twodo0.capstoneWeb.rental.dto.RentalBatchDtos;
import com.twodo0.capstoneWeb.rental.service.RentalService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

// 이전에 ImageContorller에서 이미지 업로드 후 imageId 받아서 바로 오케스트레이션

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rentals/batch")
public class RentalBatchController {

    private final RentalService rentalService;

    @PostMapping("/start/upload")
    public RentalBatchDtos.RentalStartBatchRes startRental(@RequestBody RentalBatchDtos.RentalStartBatchReq req)
    {
        return rentalService.startRental(req);
    }

    @PostMapping("/end/upload")
    public RentalBatchDtos.RentalFinishBatchRes finishRental(@RequestBody RentalBatchDtos.RentalFinishBatchReq req){
        return rentalService.finishRental(req);
    }

}
