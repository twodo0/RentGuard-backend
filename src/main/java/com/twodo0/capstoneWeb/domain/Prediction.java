package com.twodo0.capstoneWeb.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "prediction")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Prediction extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private ImageMeta image; // 하나의 이미지에 대해 여러 예측 가능

    // private OffsetDateTime createdAt;

    private String heatmapBucket; // MinIO에 저장될 히트맵의 bucket
    private String heatmapKey;//MinIO에 저장된 히트맵 이미지 이름

    private String previewBucket;
    private String previewKey;

    // 경계상자
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "prediction", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default // 빌더를 쓰면 필드 초기화값이 무시되는데, @Builder.Default를 붙이면 빌더를 사용해도 이 초기값이 유지됨
    @BatchSize(size = 10)
    private List<Detection> detections = new ArrayList<>();

    @OneToOne(fetch = FetchType.LAZY)
    private RentalSession rentalSession;

    // 연관관계 편의 메서드
    // 양방향 둘 다 초기화 + FK 세팅
    public void setDetections(List<Detection> items) {
        this.detections.clear();
        if (items != null) {
            items.forEach(this::addDetection);
        }
    }
    public void addDetection(Detection detection){
        detections.add(detection);
        detection.setPrediction(this); // 소유자 쪽 세팅
    }


    public void deleteDetection(Detection detection){
        detections.remove(detection);
        detection.setPrediction(null);
    }
}
