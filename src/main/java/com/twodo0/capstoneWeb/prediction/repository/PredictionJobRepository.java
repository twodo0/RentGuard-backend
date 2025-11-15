package com.twodo0.capstoneWeb.prediction.repository;

import com.twodo0.capstoneWeb.prediction.domain.PredictionJob;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PredictionJobRepository extends JpaRepository<PredictionJob, Long> {
}
