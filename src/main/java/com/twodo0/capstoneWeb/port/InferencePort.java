package com.twodo0.capstoneWeb.port;

import com.twodo0.capstoneWeb.dto.FastApiPredictRes;

public interface InferencePort {
    public FastApiPredictRes predictByUrl(String imageUrl, double yoloThreshold, double vitThreshold, String model, String previewUploadUrl);
    public FastApiPredictRes predictByUrl(String imageUrl, double yoloThreshold, double vitThreshold, String model, String previewUploadUrl, String heatmapUploadUrl);
}
