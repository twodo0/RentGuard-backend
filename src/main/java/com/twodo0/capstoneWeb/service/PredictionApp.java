package com.twodo0.capstoneWeb.service;

import com.twodo0.capstoneWeb.config.MinioProperties;
import com.twodo0.capstoneWeb.domain.ImageMeta;
import com.twodo0.capstoneWeb.domain.Prediction;
import com.twodo0.capstoneWeb.dto.FastApiPredictRes;
import com.twodo0.capstoneWeb.port.InferencePort;
import com.twodo0.capstoneWeb.port.PresignUrlPort;
import com.twodo0.capstoneWeb.repository.ImageRepository;
import com.twodo0.capstoneWeb.repository.PredictionRepository;
import com.twodo0.capstoneWeb.service.mapper.FastApiMapper;
import com.twodo0.capstoneWeb.storage.StorageKeyFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Transactional
@RequiredArgsConstructor
public class PredictionApp { // 실제로 사진을 저장하고, FastAPI를 호출하는 서비스
    private final MinioProperties minioProperties;
    private final ImageRepository imageRepository;
    private final PredictionRepository predictionRepository;
    private final  PresignUrlPort presignUrlPort;
    private final InferencePort fastApiAdapter;
    private final StorageKeyFactory keyFactory;

    public Long runAndSave(Long imageId, Double yoloThreshold, Double vitThreshold, String model){
        ImageMeta imageMeta = imageRepository.findById(imageId).orElseThrow();
        String rawUrl = presignUrlPort.presignGet(imageMeta.getBucket(), imageMeta.getKey());

        Prediction prediction = Prediction.builder()
                .image(imageMeta)
                .build();

        // 일단 저장해서 id 확보
        Prediction p = predictionRepository.save(prediction);

        String heatmapBucket = minioProperties.getHeatmapBucket();
        String heatmapKey = keyFactory.getHeatmapKey(p.getId());

        String previewBucket = minioProperties.getPreviewBucket();
        String previewKey = keyFactory.getPreviewKey(p.getId());

        // FastAPI 에서 직접 heatmap을 올려줘야 하기에 heatmapUrl (PUT 전용) 생성해서 던져줌
        String heatmapUploadUrl = presignUrlPort.presignPut(heatmapBucket, heatmapKey);

        String previewUploadUrl = presignUrlPort.presignPut(previewBucket, previewKey);

        //추론 결과 + heatmapId 반환
        FastApiPredictRes res = fastApiAdapter.predictByUrl(rawUrl, yoloThreshold, vitThreshold, model, previewUploadUrl, heatmapUploadUrl);

        p.setHeatmapBucket(heatmapBucket);
        p.setHeatmapKey(heatmapKey);

        p.setPreviewBucket(previewBucket);
        p.setPreviewKey(previewKey);

        p.setDetections(FastApiMapper.toDetections(res.boxes()));
        // Prediction이 Cascade ALL이므로 flush 될 때 detection INSERT

        return p.getId();
    }

}
