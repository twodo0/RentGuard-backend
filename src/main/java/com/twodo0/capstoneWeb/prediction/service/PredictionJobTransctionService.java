package com.twodo0.capstoneWeb.prediction.service;

import com.twodo0.capstoneWeb.prediction.domain.PredictionJob;
import com.twodo0.capstoneWeb.prediction.domain.enums.Status;
import com.twodo0.capstoneWeb.prediction.repository.PredictionJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class PredictionJobTransctionService {

    private final PredictionJobRepository predictionJobRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void setRunning(Long jobId){
        PredictionJob job = predictionJobRepository.findById(jobId).orElseThrow();
        job.setStatus(Status.RUNNING);
        job.setStartedAt(OffsetDateTime.now());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void markSucceeded(Long id, Long predictionId){
        PredictionJob job = predictionJobRepository.findById(id).orElseThrow();
        job.setPredictionId(predictionId);
        job.setStatus(Status.SUCCEEDED);
        job.setFinishedAt(OffsetDateTime.now());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void markFailed(Long id, Exception e) {
        PredictionJob job = predictionJobRepository.findById(id).orElseThrow();
        job.setStatus(Status.FAILED);
        job.setErrorMessage(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        job.setFinishedAt(OffsetDateTime.now());
    }
}
