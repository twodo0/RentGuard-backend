package com.twodo0.capstoneWeb.service;

import com.twodo0.capstoneWeb.domain.PredictionJob;
import com.twodo0.capstoneWeb.repository.PredictionJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RunAsyncService {
    private final PredictionJobRepository predictionJobRepository;
    private final PredictionJobTransctionService tx;
    private final PredictionApp app;

    // 별도 트랜잭션 경계에서 안전하게 처리(이 메서드가 호출될 때 항상 새로운 트랜잭션을 만듦)
    @Async("predictionExecutor")
    public void runAsync(Long jobId){
        tx.setRunning(jobId);
        try {
            PredictionJob job = predictionJobRepository.findById(jobId).orElseThrow();
            Long predictionId = app.runAndSave(job.getImageId(), job.getThreshold(), job.getModel());
            tx.markSucceeded(jobId, predictionId);
        } catch (Exception e) {
            tx.markFailed(jobId,e);
        }
    }
}
