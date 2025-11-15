package com.twodo0.capstoneWeb.rental.util;

import com.twodo0.capstoneWeb.prediction.domain.enums.DamageType;

import java.util.EnumMap;
import java.util.Map;

public final class DamageMap {
    public static int sumPositive(Map<DamageType, Integer> map){
        int t = 0;
        for (Integer v : map.values()) {
            if(v != null && v > 0) {
                t += v;
            }
        }
        return t;
    }


    public static Map<DamageType, Integer> positiveOnly(Map<DamageType, Integer> m) {
        EnumMap<DamageType, Integer> out = new EnumMap<>(DamageType.class);
        if (m == null) return out;
        m.forEach((k, v) -> {if(v != null && v > 0) out.put(k, v); });
        return out;
    }


}
