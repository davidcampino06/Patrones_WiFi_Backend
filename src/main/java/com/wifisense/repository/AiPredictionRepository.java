package com.wifisense.repository;

import com.wifisense.model.AiPrediction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface AiPredictionRepository extends JpaRepository<AiPrediction, Long> {

    @EntityGraph(attributePaths = "network")
    List<AiPrediction> findByAnomalyDetectedTrueOrderByCreatedAtDesc(Pageable pageable);

    List<AiPrediction> findByAnalysisResultIdIn(Collection<Long> analysisResultIds);

    long countByAnomalyDetectedTrueAndCreatedAtAfter(Instant since);

    long countByNetworkIdAndAnomalyDetectedTrueAndCreatedAtBetween(Long networkId, Instant from, Instant to);
}
