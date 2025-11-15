package com.twodo0.capstoneWeb.prediction.domain;

import com.twodo0.capstoneWeb.prediction.domain.enums.DamageType;
import jakarta.persistence.*;
import lombok.*;


@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClassProb {
    @Enumerated(EnumType.STRING)
    private DamageType damageType;

    private double prob;
}

