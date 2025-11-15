package com.twodo0.capstoneWeb.prediction.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import java.util.List;

// 외부 API 응답 DTO
public record DetectionDto(
        @JsonProperty("class_probs")
        @JsonSetter(nulls = Nulls.AS_EMPTY)
        List<ClassProbDto> classProbs,
        double x, double y, double w, double h) {}