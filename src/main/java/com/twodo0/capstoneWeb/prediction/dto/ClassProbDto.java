package com.twodo0.capstoneWeb.prediction.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.twodo0.capstoneWeb.prediction.domain.enums.DamageType;

public record ClassProbDto(
        @JsonProperty("label") DamageType damageType,
        @JsonProperty("prob") double prob) {
}
