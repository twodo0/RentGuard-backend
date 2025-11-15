package com.twodo0.capstoneWeb.rental.util;

import com.twodo0.capstoneWeb.prediction.domain.Prediction;
import com.twodo0.capstoneWeb.prediction.domain.enums.DamageType;
import com.twodo0.capstoneWeb.rental.dto.ConditionSummary;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class RentalAggregates {
    public static ConditionSummary aggregatePredictions(List<Prediction> predictions) {
        EnumMap<DamageType, Integer> sum = new EnumMap<>(DamageType.class);
        int total = 0;

        for (Prediction p : predictions) {
            ConditionSummary one = ConditionSummary.summarize(p);
            total += one.total();
            one.byClass().forEach((k, v) -> sum.merge(k, v, Integer::sum));
        }
        for(DamageType t : DamageType.values()) {
            sum.putIfAbsent(t, 0);
        }
        return new ConditionSummary(total, Map.copyOf(sum));
    }

    public static ConditionSummary diff(ConditionSummary start, ConditionSummary finish) {
        Map<DamageType, Integer> diff = new EnumMap<>(DamageType.class);
        int newDamageTotal = 0;

        for (DamageType t : DamageType.values()) {
            int f = finish.byClass().getOrDefault(t, 0);
            int s = start.byClass().getOrDefault(t, 0);
            int v = f - s; // byClass는 음수가 나올 수도 있음

            diff.put(t, v);
            if(v > 0) newDamageTotal += v;
        }

        return new ConditionSummary(newDamageTotal, diff);
    }

}
