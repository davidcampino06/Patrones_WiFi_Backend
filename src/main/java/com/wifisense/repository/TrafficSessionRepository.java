package com.wifisense.repository;

import com.wifisense.model.TrafficSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrafficSessionRepository extends JpaRepository<TrafficSession, Long> {

    List<TrafficSession> findByDeviceIdOrderByStartedAtDesc(Long deviceId);
}
