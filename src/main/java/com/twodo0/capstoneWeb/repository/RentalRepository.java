package com.twodo0.capstoneWeb.repository;

import com.twodo0.capstoneWeb.domain.RentalSession;
import com.twodo0.capstoneWeb.domain.enums.RentalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RentalRepository extends JpaRepository<RentalSession, Long> {
    Optional<RentalSession> findByIdAndStatus(Long id, RentalStatus status);

    boolean existsByVehicleNoAndStatus(String vehicleNo, RentalStatus status);

}
