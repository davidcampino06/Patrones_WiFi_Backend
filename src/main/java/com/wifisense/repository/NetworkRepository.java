package com.wifisense.repository;

import com.wifisense.model.Network;
import com.wifisense.model.NetworkStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface NetworkRepository extends JpaRepository<Network, Long> {

    @EntityGraph(attributePaths = {"zone", "zone.location"})
    List<Network> findAllByOrderBySsid();

    @EntityGraph(attributePaths = {"zone", "zone.location"})
    List<Network> findByStatusOrderBySsid(NetworkStatus status);

    @EntityGraph(attributePaths = {"zone", "zone.location"})
    Optional<Network> findWithZoneById(Long id);

    boolean existsByBssid(String bssid);

    @Query("select n.status as status, count(n) as total from Network n group by n.status")
    List<StatusCount> countByStatus();

    interface StatusCount {
        NetworkStatus getStatus();

        long getTotal();
    }
}
