package com.twodo0.capstoneWeb.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.twodo0.capstoneWeb.domain.Prediction;
import com.twodo0.capstoneWeb.domain.RentalSession;
import com.twodo0.capstoneWeb.domain.enums.DamageType;
import com.twodo0.capstoneWeb.domain.enums.RentalStatus;
import com.twodo0.capstoneWeb.dto.*;
import com.twodo0.capstoneWeb.repository.PredictionRepository;
import com.twodo0.capstoneWeb.repository.RentalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class RentalService {

    private final PredictionApp app;
    private final PredictionRepository predictionRepository;
    private final RentalRepository rentalRepository;
    private final ObjectMapper om;
    private final RentalTxService tx;


    public RentalStartRes startRental(RentalReq req) {
        Long predictionId = app.runAndSave(req.imageId(), req.yoloThreshold(),
                req.vitThreshold(), req.model());

        Prediction prediction = predictionRepository.findById(predictionId).orElseThrow();

        // 렌트 시작 요약
        ConditionSummary start = ConditionSummary.summarize(prediction, req.vitThreshold());

        RentalSession s = RentalSession.createStart(
                req.vehicleNo(),
                prediction,
                writeJson(start)
        );

        // 트랜잭션은 짧게 가져감
        s = tx.saveStart(s);

        return new RentalStartRes(s.getId(), s.getVehicleNo(), predictionId, start, s.getStatus()); // IN_RENT
    }

    public RentalFinishRes finishRental(RentalFinishReq req) {
        // 진행 중이던 세션 로드
        RentalSession s  = rentalRepository.findByIdAndStatus(req.rentalId(), RentalStatus.IN_RENT).orElseThrow(
                () -> new IllegalArgumentException("랜탈 정보를 찾을 수 없거나, 이미 반납되었습니다.")
        );

        if(req.vehicleNo() != null && !req.vehicleNo().equals(s.getVehicleNo())) {
            throw new IllegalArgumentException("차량 번호 불일치");
        }

        // 예측 실행
        Long finishPredictionId = app.runAndSave(req.imageId(), req.yoloThreshold(), req.vitThreshold(), req.model());
        Prediction finishPrediction = predictionRepository.findById(finishPredictionId).orElseThrow();

        ConditionSummary start = readJson(s.getStartSummaryJson(),  ConditionSummary.class);
        ConditionSummary finish = ConditionSummary.summarize(finishPrediction, req.vitThreshold());

        Map<DamageType, Integer> delta = ConditionSummary.diff(finish, start);

        String deltaJson = writeJson(delta);

        s.finish(finishPrediction, writeJson(finish), deltaJson);
        tx.saveEnd(s);

        return new RentalFinishRes(s.getId(), s.getVehicleNo(), finishPredictionId, finish, delta, s.getStatus()); // RETURNED

    }

    private String writeJson(Object o){
        try { return om.writeValueAsString(o);}
        catch (Exception e) { throw new RuntimeException("JSON 직렬화 실패", e);}
    }

    private <T> T readJson(String s, Class<T> type) {
        try{ return om.readValue(s, type); }
        catch(Exception e) { throw new RuntimeException("JSON 역직렬화 실패", e);
        }
    }
}



