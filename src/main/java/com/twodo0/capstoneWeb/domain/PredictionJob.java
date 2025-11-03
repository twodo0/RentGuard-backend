package com.twodo0.capstoneWeb.domain;

import com.twodo0.capstoneWeb.domain.enums.Status;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "prediction_job")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class PredictionJob {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private Status status;

    private Long imageId;
    private Double threshold;
    private String model;

    private Long predictionId; // 완료 시 채움

    private String errorMessage;

    private OffsetDateTime createdAt;
    private OffsetDateTime startedAt;
    private OffsetDateTime finishedAt;

}
