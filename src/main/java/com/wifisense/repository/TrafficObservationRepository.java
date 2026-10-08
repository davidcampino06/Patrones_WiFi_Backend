package com.wifisense.repository;

import com.wifisense.model.TrafficObservation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface TrafficObservationRepository extends JpaRepository<TrafficObservation, Long> {

    List<TrafficObservation> findByNetworkIdAndObservedAtBetween(Long networkId, Instant from, Instant to);

    List<TrafficObservation> findByNetworkIdOrderByObservedAtDesc(Long networkId, Pageable pageable);
}
