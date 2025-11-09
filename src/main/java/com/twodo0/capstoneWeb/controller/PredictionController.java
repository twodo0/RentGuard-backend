package com.twodo0.capstoneWeb.controller;
import com.twodo0.capstoneWeb.domain.enums.Status;
import com.twodo0.capstoneWeb.dto.PredictionDetailDto;
import com.twodo0.capstoneWeb.dto.PredictionJobCreateRequest;
import com.twodo0.capstoneWeb.dto.PredictionJobDto;
import com.twodo0.capstoneWeb.dto.PredictionRowView;
import com.twodo0.capstoneWeb.service.PredictionJobService;
import com.twodo0.capstoneWeb.service.PredictionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/predictions")
@RequiredArgsConstructor
@Slf4j
public class PredictionController {

    private final PredictionService predictionService;
    private final PredictionJobService predictionJobService;
    // 읽기, 쓰기 별도 호출 !!

    // 일단 예측 시키기
    @PostMapping("/by-image/{imageId}")
    public ResponseEntity<PredictionJobDto> createPredictionJob(
            @PathVariable Long imageId,
            @RequestParam(defaultValue = "0.2") double yoloThreshold,
            @RequestParam(defaultValue = "0.65") double vitThreshold,
            @RequestParam(defaultValue = "YOLO - ViT") String model
    ) {
        PredictionJobDto res = predictionJobService.submit(new PredictionJobCreateRequest(imageId, yoloThreshold, vitThreshold, model));

        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/predictions/jobs/{jobId}")
                .buildAndExpand(res.jobId())
                .toUri();

        return ResponseEntity
                .accepted()
                .location(location)
                .body(res);
    }

    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<?> getAsyncResult(@PathVariable Long jobId){
        var res = predictionJobService.getStatus(jobId);
        log.info("getStatus jobId={}, status={}, predId={}", jobId, res.status(), res.predictionId());

        // 다 예측 완료(200 ok)
        if(res.status() == Status.SUCCEEDED && res.predictionId() != null) {
            PredictionDetailDto detail = predictionService.getPredictionDetail(res.predictionId());
            return ResponseEntity.ok(detail);
        }
        // 예측 실패
        // 클라이언트에게 보여줄 job 상태 결과 (422 요청은 유효했으나 job에서 처리 실패...)
        else if(res.status() == Status.FAILED) {
            return ResponseEntity.unprocessableEntity().body(new ApiError(Status.FAILED, res.errorMessage()));
        }
        // 아직 진행 중(202 accepted)
        else {
            return ResponseEntity.accepted()
                    .header(HttpHeaders.RETRY_AFTER, "1")
                    .build();
        }
    }

    public record ApiError(Status status, String message) {}

    //목록 조회
    @GetMapping("/recent")
    public Page<PredictionRowView> recentPredictions(
            // 기본으로 page, size, sort 쿼리스트링을 받아서 Pageable에 넣어줌
            @PageableDefault(size = 5, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable){
        return predictionService.findRecent(pageable);
    }

}
