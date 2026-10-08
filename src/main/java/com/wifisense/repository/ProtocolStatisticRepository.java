package com.wifisense.repository;

import com.wifisense.model.ProtocolStatistic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProtocolStatisticRepository extends JpaRepository<ProtocolStatistic, Long> {

    @Query("""
            select p from ProtocolStatistic p
            where p.network.id = :networkId
              and p.periodStart = (select max(q.periodStart) from ProtocolStatistic q where q.network.id = :networkId)
            order by p.packetCount desc""")
    List<ProtocolStatistic> findLatestPeriod(Long networkId);
}
