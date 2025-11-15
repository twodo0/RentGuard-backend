package com.twodo0.capstoneWeb.rental.domain;

import com.twodo0.capstoneWeb.common.domain.Auditable;
import com.twodo0.capstoneWeb.rental.domain.enums.Phase;
import com.twodo0.capstoneWeb.rental.domain.enums.RentalStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table
@NoArgsConstructor
@Getter
@SuperBuilder
public class RentalSession extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String vehicleNo;

    @Builder.Default
    @OneToMany(mappedBy = "rentalSession", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<RentalImage> images = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private RentalStatus status;

    // 요약 / 비교 결과 (JSON 문자열 저장)
    @Lob
    @Column(columnDefinition = "TEXT")
    private String startSummaryJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String endSummaryJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String deltaJson;


    public static RentalSession createStart(String vehicleNo) {
        RentalSession r = new RentalSession();
        r.vehicleNo = vehicleNo;
        r.status = RentalStatus.IN_RENT;
        return r;
    }

    public void addImage(RentalImage img) {
        if (img == null) return;
        if (this.images == null) {
            this.images = new ArrayList<>();
        }
        this.images.add(img);
        img.setRentalSession(this);
    }

    public void setStartSummary(String startSummary) {
        this.startSummaryJson = startSummary;
    }

    public void finish(String endSummaryJson, String deltaJson) {
        this.endSummaryJson = endSummaryJson;
        this.deltaJson = deltaJson;
        this.status = RentalStatus.RETURNED;
    }

    // 편의 조회
    public List<RentalImage> getStartImage() {
        return images.stream().filter(i-> i.getPhase() == Phase.START).toList();
    }

    public List<RentalImage> getEndImage() {
        return images.stream().filter(i -> i.getPhase() == Phase.END).toList();
    }
}