package com.twodo0.capstoneWeb.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "detection")
public class Detection {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) // optional은 JPA 레벨에서 이 연관관계가 null이면 안된다는 것을 의미
    @JoinColumn(name = "prediction_id", nullable = false)
    private Prediction prediction;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "detection_per_probs", joinColumns = @JoinColumn(name = "detection_id")) // 값 타입 테이블에 생길 FK이름(부모 id와 같을 필요 X)
    @OrderBy("prob DESC ") // JPA로 조회할 때 정렬된 상태로 조회
    @BatchSize(size = 10)
    // 여러 Detection의 ClassProbs를 ID 묶음(batch)로 가져와 N+1을 줄임
    // 부모 기준 묶음
    private List<ClassProb> classProbs;

    private double x; private double y; //좌상단 (원본 픽셀 기준)
    private double w; private double h; //너비 높이
}