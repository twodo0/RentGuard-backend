package com.twodo0.capstoneWeb.domain;

import com.twodo0.capstoneWeb.domain.enums.RentalStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "rental_session",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_start_pred", columnNames = "start_prediction_id"),
                @UniqueConstraint(name = "uk_end_pred", columnNames = "end_prediction_id")
        }
)
@NoArgsConstructor
@Getter
public class RentalSession extends Auditable{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String vehicleNo;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "start_prediction_id", nullable = false, unique = true)
    private Prediction startPrediction;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "end_prediction_id", nullable = true)
    private Prediction endPrediction;

    @Enumerated(EnumType.STRING)
    private RentalStatus status;

    // 요약 / 비교 결과 (JSON 문자열 저장)
    @Lob @Column(columnDefinition = "TEXT")
    private String startSummaryJson;

    @Lob @Column(columnDefinition = "TEXT")
    private String endSummaryJson;

    @Lob @Column(columnDefinition = "TEXT")
    private String deltaJson;

    public static RentalSession createStart(String vehicleNo,
                                            Prediction startPrediction,
                                            String startSummaryJson
                                            ) {
        RentalSession r = new RentalSession();
        r.vehicleNo = vehicleNo;
        r.startPrediction = startPrediction;
        r.startSummaryJson = startSummaryJson;
        r.status = RentalStatus.IN_RENT;
        return r;
    }


    public void finish(Prediction endPrediction, String endSummaryJson, String deltaJson) {

        if(endPrediction == null){
            throw new IllegalArgumentException("endPrediction must not be null");
        }
        this.endPrediction = endPrediction;
        this.endSummaryJson = endSummaryJson;
        this.deltaJson = deltaJson;
        this.status = RentalStatus.RETURNED;
    }


}
