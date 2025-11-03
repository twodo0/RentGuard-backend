package com.twodo0.capstoneWeb.repository;

import com.twodo0.capstoneWeb.domain.Prediction;
import com.twodo0.capstoneWeb.dto.PredictionRowDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PredictionRepository extends JpaRepository<Prediction, Long> {

    // DTO 프로젝션으로 한방에 쿼리를 날리기 떄문에 N+1 안 생김
    @Query(value = """
    select new com.twodo0.capstoneWeb.dto.PredictionRowDto(
    p.id, p.createdAt, i.bucket, i.key, count(d.id)
    )
    from Prediction p join p.image i
    left join p.detections d
    group by p.id, p.createdAt, i.bucket, i.key
""",
    countQuery = "select count(p) from Prediction p")
    Page<PredictionRowDto> findRecentRows(Pageable pageable);


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
