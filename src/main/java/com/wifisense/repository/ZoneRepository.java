package com.wifisense.repository;

import com.wifisense.model.Zone;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ZoneRepository extends JpaRepository<Zone, Long> {

    @EntityGraph(attributePaths = "location")
    List<Zone> findByLocationIdOrderByName(Long locationId);

    @EntityGraph(attributePaths = "location")
    List<Zone> findAllByOrderByName();
}
