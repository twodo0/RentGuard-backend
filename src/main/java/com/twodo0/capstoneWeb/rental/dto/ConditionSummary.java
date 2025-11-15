package com.twodo0.capstoneWeb.rental.dto;

import com.twodo0.capstoneWeb.prediction.domain.ClassProb;
import com.twodo0.capstoneWeb.prediction.domain.Detection;
import com.twodo0.capstoneWeb.prediction.domain.Prediction;
import com.twodo0.capstoneWeb.prediction.domain.enums.DamageType;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;

public record ConditionSummary(
        int total, Map<DamageType, Integer> byClass
) {
    public static ConditionSummary summarize(Prediction p) {
        Map<DamageType, Integer> m = new EnumMap<>(DamageType.class);
        int total = 0;
        if (p != null && p.getDetections() != null) {
            for (Detection d : p.getDetections()) {
                var probs = d.getClassProbs();
                if (probs == null || probs.isEmpty()) {
                    continue;
                }

                // class_probs 중 top1 찾기
                ClassProb top1 = probs.stream()
                        .max(Comparator.comparingDouble(ClassProb::getProb))
                        .orElse(null);

                // 존재하면 누적(+1), 없으면 1로 삽입
                m.merge(top1.getDamageType(), 1, Integer::sum);
                total++;
            }
        }

        // 이후 단계에서 키 누락으로 인한 NPE 방지용 (0으로 채우기)
        for (DamageType t : DamageType.values()) {
            m.putIfAbsent(t, 0);
        }

        return new ConditionSummary(total, Map.copyOf(m));

    }
}
