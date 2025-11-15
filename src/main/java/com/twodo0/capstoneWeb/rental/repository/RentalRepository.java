package com.twodo0.capstoneWeb.rental.repository;

import com.twodo0.capstoneWeb.rental.domain.RentalSession;
import com.twodo0.capstoneWeb.rental.domain.enums.RentalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RentalRepository extends JpaRepository<RentalSession, Long> {
    boolean existsByVehicleNoAndStatus(String vehicleNo, RentalStatus status);

    @Query("""
        select distinct r
        from RentalSession r
        left join fetch r.images ri
        left join fetch ri.prediction p
        where r.id = :id
""")
    Optional<RentalSession> findByIdWithImagesAndPredictions(@Param("id") Long id);

    @Query("""
        select distinct r
        from RentalSession r
        left join fetch r.images ri
        left join fetch ri.prediction p
        left join fetch p.image im
        where r.id = :id
""")
    Optional<RentalSession> findByIdDeep(@Param("id") Long id);
}
