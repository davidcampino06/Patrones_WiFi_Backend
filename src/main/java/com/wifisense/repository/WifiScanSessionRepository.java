package com.wifisense.repository;

import com.wifisense.model.ScanOrigin;
import com.wifisense.model.ScanStatus;
import com.wifisense.model.WifiScanSession;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WifiScanSessionRepository extends JpaRepository<WifiScanSession, Long> {

    /** The scan waiting or running on a network (there is at most one). */
    Optional<WifiScanSession> findFirstByNetworkIdAndStatusIn(Long networkId, Collection<ScanStatus> statuses);

    /** The newest scan of a network, whatever its state (also tells when the schedule last ran). */
    Optional<WifiScanSession> findFirstByNetworkIdOrderByRequestedAtDesc(Long networkId);

    /** The newest scan of one state: the last completed one is what the page shows. */
    Optional<WifiScanSession> findFirstByNetworkIdAndStatusOrderByFinishedAtDesc(Long networkId, ScanStatus status);

    /** The newest manual scan, to keep the button from being pressed in a loop. */
    Optional<WifiScanSession> findFirstByNetworkIdAndOriginOrderByRequestedAtDesc(Long networkId, ScanOrigin origin);

    List<WifiScanSession> findByNetworkIdOrderByRequestedAtDesc(Long networkId, Pageable pageable);

    Optional<WifiScanSession> findByIdAndNetworkId(Long id, Long networkId);

    @Modifying
    @Query("delete from WifiScanSession s where s.requestedAt < :before")
    int deleteOlderThan(@Param("before") Instant before);
}
