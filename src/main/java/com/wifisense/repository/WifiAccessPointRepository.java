package com.wifisense.repository;

import com.wifisense.model.WifiAccessPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface WifiAccessPointRepository extends JpaRepository<WifiAccessPoint, Long> {

    /** The access points of one scan that the network had already seen, in a single query. */
    List<WifiAccessPoint> findByNetworkIdAndBssidFingerprintIn(Long networkId, Collection<String> fingerprints);

    @Modifying
    @Query("delete from WifiAccessPoint a where a.lastSeenAt < :before")
    int deleteNotSeenSince(@Param("before") Instant before);
}
