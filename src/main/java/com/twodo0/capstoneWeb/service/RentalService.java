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

        boolean alreadyInRent = rentalRepository.existsByVehicleNoAndStatus(req.vehicleNo(), RentalStatus.IN_RENT);
        if (alreadyInRent) {
            throw new IllegalArgumentException("이미 대여 중인 차량입니다.");
        }

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

        RentalSession s = rentalRepository.findById(req.rentalId()).orElseThrow(
                () -> new IllegalArgumentException("렌탈 정보를 찾을 수 없습니다.")
        );

        if(!req.vehicleNo().equals(s.getVehicleNo())) {
            throw new IllegalArgumentException("차량 번호가 일치하지 않습니다.");
        }

        if(s.getStatus() != RentalStatus.IN_RENT) {
            throw new IllegalArgumentException("이미 반납된 차량입니다.");
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

    // JSON 문자열 s를 받아서, type으로 넘겨준 클래스 타입으로 파싱해서 객체로 돌려줌
    // 제너릭 -> 원하는 타입만 명시해주면 JSON을 원하는 타입으로 바꿔줌
    private <T> T readJson(String s, Class<T> type) {
        try{ return om.readValue(s, type); }
        catch(Exception e) { throw new RuntimeException("JSON 역직렬화 실패", e);
        }
    }
}



