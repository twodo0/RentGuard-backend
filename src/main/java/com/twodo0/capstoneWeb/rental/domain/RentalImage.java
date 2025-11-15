package com.twodo0.capstoneWeb.rental.domain;

import com.twodo0.capstoneWeb.common.domain.Auditable;
import com.twodo0.capstoneWeb.prediction.domain.Prediction;
import com.twodo0.capstoneWeb.rental.domain.enums.ImageSlot;
import com.twodo0.capstoneWeb.rental.domain.enums.Phase;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_rental_phase_slot",
                columnNames = {"rental_session_id", "phase", "image_slot"}
        ),
                @UniqueConstraint(
                        name = "uk_prediction_id",
                        columnNames = {"prediction_id"}
                )
}
)
@Getter @Setter
@NoArgsConstructor
public class RentalImage extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rental_session_id", nullable = false)
    private RentalSession rentalSession;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prediction_id", nullable = false, unique = true)
    private Prediction prediction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Phase phase;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImageSlot imageSlot;

    public static RentalImage of(RentalSession session, Phase phase, ImageSlot imageSlot, Prediction prediction) {
        RentalImage r = new RentalImage();
        r.phase = phase;
        r.prediction = prediction;
        r.rentalSession = session;
        r.imageSlot = imageSlot;

        return r;
    }

}
