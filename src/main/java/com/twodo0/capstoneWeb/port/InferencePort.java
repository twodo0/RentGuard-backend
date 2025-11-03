package com.twodo0.capstoneWeb.port;

import com.twodo0.capstoneWeb.dto.FastApiPredictRes;

public interface InferencePort {
    public FastApiPredictRes predictByUrl(String imageUrl, double threshold, String model);
    public FastApiPredictRes predictByUrl(String imageUrl, double threshold, String model, String heatmapUploadUrl);
}
