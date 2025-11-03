package com.twodo0.capstoneWeb.service.mapper;


import com.twodo0.capstoneWeb.domain.ClassProb;
import com.twodo0.capstoneWeb.domain.Detection;
import com.twodo0.capstoneWeb.dto.FastApiPredictRes;

import java.util.List;

public class FastApiMapper {

    public static List<ClassProb> toClassProbs(List<FastApiPredictRes.ApiClassProbDto> src){
        if (src == null || src.isEmpty()) return List.of();
        return src.stream().
                map(c -> new ClassProb(c.damageType(), c.prob()))
                .toList();
    }

    public static List<Detection> toDetections(List<FastApiPredictRes.BoxDto> src) {
        if(src == null || src.isEmpty()) return List.of();

        return src.stream()
                .map(b -> {
            double x = safeDouble(b.x()); double y = safeDouble(b.y());
            double w = safeDouble(b.w()); double h = safeDouble(b.h());
            List<ClassProb> probs = toClassProbs(b.classProbs());

            return Detection.builder()
                    .classProbs(probs)
                    .x(x)
                    .y(y)
                    .w(w)
                    .h(h).build();
        }).toList();
    }

    private static double safeDouble(Double v) {

        return (v==null || !Double.isFinite(v)) ? 0.0 : v;
    }
}
