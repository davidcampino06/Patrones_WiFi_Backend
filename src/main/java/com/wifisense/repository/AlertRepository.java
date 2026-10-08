package com.wifisense.repository;

import com.wifisense.model.Alert;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    @EntityGraph(attributePaths = "network")
    List<Alert> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = "network")
    List<Alert> findByStatusOrderByCreatedAtDesc(Alert.Status status, Pageable pageable);

    List<Alert> findByNetworkIdAndStatusNot(Long networkId, Alert.Status status);

    long countByStatusNot(Alert.Status status);

    long countByNetworkIdAndCreatedAtBetween(Long networkId, Instant from, Instant to);
}
