package com.twodo0.capstoneWeb.prediction.repository;

import com.twodo0.capstoneWeb.prediction.domain.Prediction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PredictionRepository extends JpaRepository<Prediction, Long> {


    // detections까지만 fetch-join (한 번에 하나의 컬렉션만 fetch-join)
    // left join fetch p.detections -> detections를 같은 SELECT로 적재
    // fetch join으로 가져온 엔티티는 필드가 채워진 상태(프록시가 아닌 초기화된 상턔)로 리턴.
    @Query("""
    select distinct p
    from Prediction p
    left join fetch p.image
    left join fetch p.detections d
    where p.id = :id
""")
   Optional<Prediction> findWithDetectionsById(Long id);

}
