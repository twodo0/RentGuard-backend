package com.twodo0.capstoneWeb.rental.service;

import com.twodo0.capstoneWeb.prediction.domain.enums.DamageType;
import com.twodo0.capstoneWeb.rental.domain.RentalSession;
import com.twodo0.capstoneWeb.rental.domain.enums.Phase;
import com.twodo0.capstoneWeb.rental.domain.enums.RentalStatus;
import com.twodo0.capstoneWeb.rental.dto.RentalDetailDto;
import com.twodo0.capstoneWeb.rental.dto.RentalImageDto;
import com.twodo0.capstoneWeb.rental.dto.RentalRowView;
import com.twodo0.capstoneWeb.rental.mapper.RentalViewMapper;
import com.twodo0.capstoneWeb.rental.repository.RentalRepository;
import com.twodo0.capstoneWeb.rental.util.JsonMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static com.twodo0.capstoneWeb.rental.util.DamageMap.positiveOnly;
import static com.twodo0.capstoneWeb.rental.util.DamageMap.sumPositive;

@Service
@RequiredArgsConstructor
public class RentalQueryService {

    private final RentalRepository rentalRepository;
    private final RentalViewMapper rentalViewMapper;
    private final JsonMapper jsonMapper;

    @Transactional(readOnly = true)
    public Page<RentalRowView> recent(Pageable pageable){
        Page<RentalSession> page = rentalRepository.findAll(pageable);
        return page.map(r ->{
            // 상태별 finishedAt 처리
            var finishedAt = (r.getStatus() == RentalStatus.RETURNED) ? r.getLastModifiedDate() : null;

            EnumMap<DamageType,Integer> deltaMap = jsonMapper.getDamageMap(r.getDeltaJson());

            // 상태별 newDamageTotal 처리
            // 증가분만
            Integer newDamageTotal = (r.getStatus() == RentalStatus.RETURNED)
                    ? sumPositive(deltaMap)
                    : null;

            return new RentalRowView(
                    r.getId(),
                    r.getVehicleNo(),
                    r.getStatus(),
                    r.getCreatedAt(), // 렌트 시작일
                    finishedAt, // 반납일 (상태에 따라 null)
                    newDamageTotal // <- RETURNED 아닐 땐 null
            );
        });
    }

    @Transactional(readOnly = true)
    public RentalDetailDto detail(Long rentalId, Phase phase) {
        RentalSession s = rentalRepository.findByIdDeep(rentalId)
                .orElseThrow(() -> new IllegalArgumentException("렌탈 정보를 찾을 수 없습니다."));

        // controller에서 phase를 비워서 보낼 떄 방지
        Phase effective = (phase != null)
                ? phase
                : (s.getStatus() == RentalStatus.RETURNED
                ? Phase.END
                : Phase.START);

        // 렌탈중이거나, 시작 페이스의 사진일 때
        boolean onlyStart = ((s.getStatus() == RentalStatus.IN_RENT) || (effective == Phase.START));

        Map<DamageType, Integer> startMap = jsonMapper.getDamageMap(s.getStartSummaryJson());
        Map<DamageType, Integer> finishMap = jsonMapper.getDamageMap(s.getEndSummaryJson());
        Map<DamageType, Integer> deltaMap = jsonMapper.getDamageMap(s.getDeltaJson());

        Integer startTotal = sumPositive(startMap);
        // start인 상태의 세션을 조회했을 떄를 위해 null로 초기화
        Integer finishTotal = null;
        Integer newDamageTotal = null;

        List<RentalImageDto> statImages = s.getImages().stream()
                .filter(ri -> ri.getPhase() == Phase.START)
                .map(rentalViewMapper::toImageDto)
                .toList();

        List<RentalImageDto> finishImages = s.getImages().stream()
                .filter( ri -> ri.getPhase() == Phase.END)
                .map(rentalViewMapper::toImageDto)
                .toList();


        // 반납이 안되었을 수도 있으므로
        OffsetDateTime finishedAt = (s.getStatus() == RentalStatus.RETURNED)
                ? s.getLastModifiedDate()
                : null;

        Map<DamageType, Integer> outFinishMap;
        Map<DamageType, Integer> outDeltaMap;
        List<RentalImageDto> outFinishImages;

        if(onlyStart){
            outFinishMap = Map.of();
            outDeltaMap = Map.of();
            outFinishImages = List.of();
        } else {
            finishTotal = sumPositive(finishMap);
            newDamageTotal = sumPositive(deltaMap);
            outFinishMap = finishMap;
            outDeltaMap = positiveOnly(deltaMap);
            outFinishImages = finishImages; // 반납이 안된 상태라면 null
        }

        return new RentalDetailDto(
                s.getId(),
                s.getVehicleNo(),
                s.getStatus(),
                s.getCreatedAt(),
                finishedAt,
                startMap,
                outFinishMap,
                outDeltaMap,
                startTotal,
                finishTotal,
                newDamageTotal,
                statImages,
                outFinishImages
        );
    }


}
