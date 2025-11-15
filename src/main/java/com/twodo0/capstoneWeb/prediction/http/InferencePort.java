package com.twodo0.capstoneWeb.prediction.http;

import com.twodo0.capstoneWeb.prediction.dto.FastApiPredictRes;

public interface InferencePort {
    public FastApiPredictRes predictByUrl(String imageUrl, double yoloThreshold, String heatmapUploadUrl);
}
