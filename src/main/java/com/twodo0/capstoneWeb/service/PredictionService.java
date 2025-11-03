package com.twodo0.capstoneWeb.service;

import com.twodo0.capstoneWeb.domain.Prediction;
import com.twodo0.capstoneWeb.dto.*;
import com.twodo0.capstoneWeb.port.PresignUrlPort;
import com.twodo0.capstoneWeb.repository.PredictionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly=true)
public class PredictionService {

    private final PredictionRepository predictionRepository;
    private final PresignUrlPort presignUrlPort;

    // 최근 등록 조회 (간단 요약)
    public Page<PredictionRowView> findRecent(Pageable pageable) {
        return predictionRepository.findRecentRows(pageable)
                .map(p ->{
                    var url = presignUrlPort.presignGet(p.imageBucket(), p.imageKey());
                    return new PredictionRowView(
                            p.predictionId(),
                            p.createdAt(),
                            presignUrlPort.presignGet(p.imageBucket(), p.imageKey()),
                            p.detectionCount());
                });
    }

    // id로 디테일한 조회 (세부 정보)
    public PredictionDetailDto getPredictionDetail(Long predictionId){
        Prediction p = getPredictionFetch(predictionId);

        List<DetectionDto> detectionDtos = Optional.ofNullable(p.getDetections()).orElse(List.of())
                .stream()
                .map(d -> {
                    var prob = Optional.ofNullable(d.getClassProbs()).orElse(List.of())
                        .stream()
                        .map(c -> new ClassProbDto(c.getDamageType(), c.getProb())).toList();
                return new DetectionDto(prob, d.getX(), d.getY(), d.getW(), d.getH());
                })
                .toList();

        String rawUrl = (p.getImage() != null)
                ? presignUrlPort.presignGet(p.getImage().getBucket(), p.getImage().getKey())
                : null;

        String heatmapUrl = (p.getHeatmapBucket() != null && p.getHeatmapKey() != null)
                ? presignUrlPort.presignGet(p.getHeatmapBucket(), p.getHeatmapKey())
                : null;

        return PredictionDetailDto.builder()
                .predictionId(p.getId())
                .createdAt(p.getCreatedAt())
                .heatMapUrl(heatmapUrl)
                .rawUrl(rawUrl)
                .detections(detectionDtos)
                .width((p.getImage() == null) ? null : p.getImage().getWidth())
                .height((p.getImage() == null) ? null : p.getImage().getHeight()) // 원본 width, height도 던져줘야 함
                .build();
    }

    public Prediction getPredictionFetch(Long id) {
        // p, p.image, p.detections는 한 방(fetch join)으로 메모리에 올라옴.
        // 단, d.classProbs는 LAZY이므로 아직 DB에서 안 읽음
        Prediction p = predictionRepository.findWithDetectionsById(id).orElseThrow();

        // 2단계 : 값타입 컬렉션(classProbs)을 배치로 로딩 트리거
        // @BatchSize(size=10)으로 다수의 detection에 대해 classProbs를 묶어서 적은 횟수의 쿼리로 가져옴

        //classProbs는 LAZY 컬렉션임. 아직 DB에서 안 읽음
        // LAZY 컬렉션은 "처음 접근할 때" 초기화. size(), isEmpty(), 실제 반복 등 아무 접근이든 트리거를 초기화해서 불러옴
        p.getDetections().forEach(d -> d.getClassProbs().size());
        return p;
    }
}