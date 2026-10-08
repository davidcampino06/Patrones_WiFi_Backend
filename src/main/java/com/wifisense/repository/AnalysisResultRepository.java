package com.wifisense.repository;

import com.wifisense.model.AnalysisResult;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, Long> {

    @EntityGraph(attributePaths = {"network", "requestedBy"})
    List<AnalysisResult> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"network", "requestedBy"})
    List<AnalysisResult> findByNetworkIdOrderByCreatedAtDesc(Long networkId, Pageable pageable);

    long countByNetworkIdAndCreatedAtBetween(Long networkId, Instant from, Instant to);
}
