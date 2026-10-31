package com.wifisense.repository;

import com.wifisense.model.WifiScanObservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WifiScanObservationRepository extends JpaRepository<WifiScanObservation, Long> {

    /** What one scan saw, with each access point, strongest signal first (unknown signal last). */
    @Query("select o from WifiScanObservation o join fetch o.accessPoint "
            + "where o.scan.id = :scanId order by o.signalPercent desc nulls last, o.id")
    List<WifiScanObservation> findByScan(@Param("scanId") Long scanId);
}
