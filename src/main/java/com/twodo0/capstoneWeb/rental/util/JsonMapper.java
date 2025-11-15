package com.twodo0.capstoneWeb.rental.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.twodo0.capstoneWeb.prediction.domain.enums.DamageType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.EnumMap;

@Component
@RequiredArgsConstructor
public class JsonMapper {

    private final ObjectMapper om;
    private static final TypeReference<EnumMap<DamageType, Integer>> DAMAGE_MAP_TR = new TypeReference<>() {};

    public String writeJson(Object o) {
        try {
            return om.writeValueAsString(o);
        } catch (Exception e) {
            throw new RuntimeException("JSON 직렬화 실패", e);
        }
    }

    // JSON 문자열 s를 받아서, type으로 넘겨준 클래스 타입으로 파싱해서 객체로 돌려줌
    // 제너릭 -> 원하는 타입만 명시해주면 JSON을 원하는 타입으로 바꿔줌
    public <T> T readJson(String s, Class<T> type) {
        try {
            return om.readValue(s, type);
        } catch (Exception e) {
            throw new RuntimeException("JSON 역직렬화 실패", e);
        }
    }

    public <T> T readJson(String s, TypeReference<T> ref) {
        try {
            return om.readValue(s, ref);
        } catch (Exception e) {
            throw new RuntimeException("JSON 역직렬화 실패", e);

        }
    }

    public EnumMap<DamageType, Integer> getDamageMap(String s) {
        if(s == null || s.isBlank()) return new EnumMap<>(DamageType.class);
        EnumMap<DamageType, Integer> m = readJson(s, DAMAGE_MAP_TR);
        return m;
    }
}