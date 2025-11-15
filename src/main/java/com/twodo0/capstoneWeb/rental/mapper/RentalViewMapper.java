package com.twodo0.capstoneWeb.rental.mapper;

import com.twodo0.capstoneWeb.common.port.PresignUrlPort;
import com.twodo0.capstoneWeb.prediction.domain.ClassProb;
import com.twodo0.capstoneWeb.prediction.domain.Detection;
import com.twodo0.capstoneWeb.prediction.domain.Prediction;
import com.twodo0.capstoneWeb.prediction.domain.enums.DamageType;
import com.twodo0.capstoneWeb.prediction.dto.ClassProbDto;
import com.twodo0.capstoneWeb.prediction.dto.DetectionDto;
import com.twodo0.capstoneWeb.rental.domain.RentalImage;
import com.twodo0.capstoneWeb.rental.dto.ConditionSummary;
import com.twodo0.capstoneWeb.rental.dto.RentalImageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RentalViewMapper {
    private final PresignUrlPort presign;

    public RentalImageDto toImageDto(RentalImage ri) {
        Prediction p = ri.getPrediction();

        if(p == null) {
            return new RentalImageDto(
                    ri.getImageSlot(),
                    null,
                    null,
                    null,
                    Map.of(),
                    List.of()
            );
        }

        String rawUrl = presign.presignGet(p.getImage().getBucket(), p.getImage().getKey());

        String heatmapUrl = (p.getHeatmapBucket() != null && p.getHeatmapKey() != null)
                        ? presign.presignGet(p.getHeatmapBucket(), p.getHeatmapKey())
                        : null;

        Map<DamageType, Integer> byImg = ConditionSummary.summarize(p).byClass();

        List<DetectionDto> detectionDtos = p.getDetections().stream()
                .map(d -> new DetectionDto(
                        d.getClassProbs().stream()
                                .map(c -> new ClassProbDto(c.getDamageType(), c.getProb()))
                                .toList(),
                        d.getX(),
                        d.getY(),
                        d.getW(),
                        d.getH()

                ))
                .toList();


        return new RentalImageDto(
                ri.getImageSlot(),
                p.getId(),
                rawUrl,
                heatmapUrl,
                byImg,
                detectionDtos
        );
    }
}
