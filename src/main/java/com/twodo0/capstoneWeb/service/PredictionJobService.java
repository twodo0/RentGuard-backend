package com.twodo0.capstoneWeb.service;


import com.twodo0.capstoneWeb.config.AiProperties;
import com.twodo0.capstoneWeb.domain.PredictionJob;
import com.twodo0.capstoneWeb.domain.enums.Status;
import com.twodo0.capstoneWeb.dto.PredictionJobCreateRequest;
import com.twodo0.capstoneWeb.dto.PredictionJobDto;
import com.twodo0.capstoneWeb.repository.PredictionJobRepository;
import com.twodo0.capstoneWeb.service.mapper.ToResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.OffsetDateTime;



@Service
@RequiredArgsConstructor

public class PredictionJobService {
    private final PredictionJobRepository predictionJobRepository;
    private final ThreadPoolTaskExecutor predictionExecutor;
    private final RunAsyncService runAsyncService;
    private final PredictionApp app;
    private final AiProperties probs;

    // 던져주고 일단 바로 return
    @Transactional
    public PredictionJobDto submit(PredictionJobCreateRequest req){
        double th = (req.threshold() != null) ? req.threshold() : probs.thresholdDefault();
        String model = (req.model() != null) ? req.model() : probs.modelDefault();

        PredictionJob job = PredictionJob.builder()
                .imageId(req.imageId())
                .threshold(th)
                .model(model)
                .status(Status.QUEUED)
                .createdAt(OffsetDateTime.now())
                .build();

        predictionJobRepository.save(job);

        // 현재 트랜잭션이 커밋된 이후에 실행될 콜백을 등록
        // flush 안된 상태에서 getById 하면 반영이 안될 수도 ..
        // ID만 캡쳐해서 스레드풀에 등록
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                runAsyncService.runAsync(job.getId());
            }
        });

        return ToResponseDto.toDto(job);
    }

    @Transactional(readOnly = true)
    public PredictionJobDto getStatus(Long jobId){
        var job = predictionJobRepository.findById(jobId).orElseThrow();
        return ToResponseDto.toDto(job);
    }

}
