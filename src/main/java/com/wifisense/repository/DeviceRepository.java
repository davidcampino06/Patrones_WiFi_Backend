package com.wifisense.repository;

import com.wifisense.model.Device;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    @EntityGraph(attributePaths = "network")
    List<Device> findAllByOrderByLastSeenAtDesc();

    @EntityGraph(attributePaths = "network")
    List<Device> findByNetworkIdOrderByLastSeenAtDesc(Long networkId);

    boolean existsByMacAddress(String macAddress);
}
