package com.twodo0.capstoneWeb.rental.service;

import com.twodo0.capstoneWeb.common.config.AiProperties;
import com.twodo0.capstoneWeb.prediction.domain.Prediction;
import com.twodo0.capstoneWeb.rental.domain.RentalImage;
import com.twodo0.capstoneWeb.rental.domain.RentalSession;
import com.twodo0.capstoneWeb.prediction.domain.enums.DamageType;
import com.twodo0.capstoneWeb.rental.domain.enums.ImageSlot;
import com.twodo0.capstoneWeb.rental.domain.enums.Phase;
import com.twodo0.capstoneWeb.rental.domain.enums.RentalStatus;
import com.twodo0.capstoneWeb.prediction.service.PredictionApp;
import com.twodo0.capstoneWeb.prediction.service.RentalTxService;
import com.twodo0.capstoneWeb.prediction.repository.PredictionRepository;
import com.twodo0.capstoneWeb.rental.dto.*;
import com.twodo0.capstoneWeb.rental.repository.RentalRepository;
import com.twodo0.capstoneWeb.rental.util.JsonMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static com.twodo0.capstoneWeb.rental.util.DamageMap.positiveOnly;
import static com.twodo0.capstoneWeb.rental.util.RentalAggregates.*;
import static com.twodo0.capstoneWeb.rental.util.RentalAggregates.aggregatePredictions;

@Service
@RequiredArgsConstructor
public class RentalService {

    private final PredictionApp app;
    private final PredictionRepository predictionRepository;
    private final RentalRepository rentalRepository;
    private final RentalTxService tx;
    private final AiProperties aiProp;
    private final JsonMapper jsonMapper;

    @Transactional
    public RentalBatchDtos.RentalStartBatchRes startRental(RentalBatchDtos.RentalStartBatchReq req) {

        final String vehicleNo = req.vehicleNo();

        final double thr = (req.yoloThreshold() != null)
                ? req.yoloThreshold()
                : aiProp.thresholdDefault();

        final List<RentalBatchDtos.ImageItem> images = Objects.requireNonNullElseGet(req.images(), List::of);

        validateImages(images);

        // 차량 번호 검증
        if(vehicleNo == null || vehicleNo.isEmpty()) {
            throw new IllegalArgumentException("차량번호를 입력해주세요.");
        }

        // 이미 대여 중 차량 여부 확인
        boolean alreadyInRent = rentalRepository.existsByVehicleNoAndStatus(req.vehicleNo(), RentalStatus.IN_RENT);
        if (alreadyInRent) {
            throw new IllegalArgumentException("이미 대여 중인 차량입니다.");
        }

        // 세션 생성
        RentalSession session = RentalSession.createStart(vehicleNo);
        session = tx.saveStart(session);

        // 각 이미지에 대한 예측
        List<RentalBatchDtos.ImageResult> results = new ArrayList<>(); // (slot, predictionId)
        List<Prediction> startPredictions = new ArrayList<>();

        for (RentalBatchDtos.ImageItem imageItem : images) {
            Long predictionId = app.runAndSave(imageItem.imageId(), thr);
            Prediction p = predictionRepository.findById(predictionId).orElseThrow();

            RentalImage ri = RentalImage.of(session, Phase.START, imageItem.slot(), p);
            session.addImage(ri);

            results.add(new RentalBatchDtos.ImageResult(imageItem.slot(), predictionId));
            startPredictions.add(p);
        }

        // 세션 전체 요약 집계
        ConditionSummary startSummary = aggregatePredictions(startPredictions);

        // 요약 map 저장
        session.setStartSummary(jsonMapper.writeJson(startSummary.byClass()));

        Integer totalDamage = startSummary.total();

        return new RentalBatchDtos.RentalStartBatchRes(
                session.getId(),
                vehicleNo,
                totalDamage,
                startSummary.byClass(),
                results,
                session.getStatus()
                );
    }



    @Transactional
    public RentalBatchDtos.RentalFinishBatchRes finishRental(RentalBatchDtos.RentalFinishBatchReq req) {

        final String vehicleNo = req.vehicleNo();

        final double thr = (req.yoloThreshold() == null)
                ? aiProp.thresholdDefault()
                : req.yoloThreshold();

        final List<RentalBatchDtos.ImageItem> images = Objects.requireNonNullElseGet(req.images(), List::of);

        validateImages(images);

        RentalSession session = rentalRepository.findByIdWithImagesAndPredictions(req.rentalId()).orElseThrow(
                () -> new IllegalArgumentException("렌탈 정보를 찾을 수 없습니다.")
        );

        if(vehicleNo == null || vehicleNo.isEmpty()) {
            throw new IllegalArgumentException("차량 번호를 입력해주세요.");
        }

        if(!req.vehicleNo().equals(session.getVehicleNo())) {
            throw new IllegalArgumentException("차량 번호가 일치하지 않습니다.");
        }

        if(session.getStatus() != RentalStatus.IN_RENT) {
            throw new IllegalArgumentException("이미 반납된 차량입니다.");
        }

        List<RentalBatchDtos.ImageResult> results = new ArrayList<>(); // slot, predictionId
        List<Prediction> finishPredictions = new ArrayList<>();

        for(RentalBatchDtos.ImageItem image : images) {
            Long PredictionId = app.runAndSave(image.imageId(), thr);
            Prediction p = predictionRepository.findById(PredictionId).orElseThrow();

            RentalImage ri = RentalImage.of(session, Phase.END, image.slot(), p);
            session.addImage(ri);

            results.add(new RentalBatchDtos.ImageResult(image.slot(), PredictionId));
            finishPredictions.add(p);
        }

        ConditionSummary startSummary = aggregatePredictionsFromSession(session, Phase.START);
        ConditionSummary finishSummary = aggregatePredictions(finishPredictions);
        ConditionSummary deltaSummary = diff(startSummary, finishSummary);

        Integer totalDamage = finishSummary.total();

        session.finish(jsonMapper.writeJson(finishSummary.byClass()), jsonMapper.writeJson(deltaSummary.byClass()));

        // 손상 증가분만 내려보냄
        Map<DamageType, Integer> deltaPositive = positiveOnly(deltaSummary.byClass());

        return new RentalBatchDtos.RentalFinishBatchRes(
                session.getId(),
                vehicleNo,
                totalDamage,
                finishSummary.byClass(),
                deltaPositive,
                results,
                session.getStatus()
        );
    }

    // 헬퍼
    private void validateImages(List<RentalBatchDtos.ImageItem> images) {
        if(images.isEmpty()) throw new IllegalArgumentException("파일이 없습니다.");
        if(images.size() > 4) throw new IllegalArgumentException("최대 4장까지 업로드 가능합니다.");

        //슬롯 중복 방지
        EnumSet<ImageSlot> seen = EnumSet.noneOf(ImageSlot.class);
        for (RentalBatchDtos.ImageItem imageItem : images) {
            if (imageItem.slot() == null) {
                throw new IllegalArgumentException("슬롯을 설정해주세요.");
            }
            if (imageItem.imageId() == null || imageItem.imageId() <= 0) {
                throw new IllegalArgumentException("유효하지 않은 이미지ID입니다.");
            }
            if(!seen.add(imageItem.slot())) {
                throw new IllegalArgumentException("중복된 슬롯 : " + imageItem.slot());
            }
        }
    }

    private ConditionSummary aggregatePredictionsFromSession(RentalSession s, Phase phase) {
        List<Prediction> preds = s.getImages().stream()
                .filter(ri -> ri.getPhase() == phase)
                .map(RentalImage::getPrediction)
                .toList();
        return aggregatePredictions(preds);
    }
}



