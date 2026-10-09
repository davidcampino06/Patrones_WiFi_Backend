package com.wifisense.service;

import com.wifisense.dto.MeasurementResponse;
import com.wifisense.dto.ProtocolStatisticResponse;
import com.wifisense.dto.TrafficObservationResponse;
import com.wifisense.repository.MeasurementRepository;
import com.wifisense.repository.NetworkRepository;
import com.wifisense.repository.ProtocolStatisticRepository;
import com.wifisense.repository.TrafficObservationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** Read side of measurement history, traffic and protocol statistics. */
@Service
@Transactional(readOnly = true)
public class MeasurementService {

    static final int MAX_LIMIT = 1000;
    private static final Duration DEFAULT_RANGE = Duration.ofHours(24);

    private final MeasurementRepository measurements;
    private final TrafficObservationRepository traffic;
    private final ProtocolStatisticRepository protocols;
    private final NetworkRepository networks;
    private final Clock clock;

    public MeasurementService(MeasurementRepository measurements, TrafficObservationRepository traffic,
                              ProtocolStatisticRepository protocols, NetworkRepository networks, Clock clock) {
        this.measurements = measurements;
        this.traffic = traffic;
        this.protocols = protocols;
        this.networks = networks;
        this.clock = clock;
    }

    /** Newest first; defaults to the last 24 hours. */
    public List<MeasurementResponse> history(Long networkId, Instant from, Instant to, int limit) {
        requireNetwork(networkId);
        Instant end = to == null ? Instant.now(clock) : to;
        Instant start = from == null ? end.minus(DEFAULT_RANGE) : from;
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("'from' must be before 'to'");
        }
        return measurements.findByNetworkIdAndMeasuredAtBetweenOrderByMeasuredAtDesc(networkId, start, end,
                        PageRequest.of(0, clamp(limit)))
                .stream().map(MeasurementResponse::from).toList();
    }

    public List<TrafficObservationResponse> traffic(Long networkId, int limit) {
        requireNetwork(networkId);
        return traffic.findByNetworkIdOrderByObservedAtDesc(networkId, PageRequest.of(0, clamp(limit)))
                .stream().map(TrafficObservationResponse::from).toList();
    }

    public List<ProtocolStatisticResponse> latestProtocols(Long networkId) {
        requireNetwork(networkId);
        return protocols.findLatestPeriod(networkId).stream().map(ProtocolStatisticResponse::from).toList();
    }

    private void requireNetwork(Long networkId) {
        if (!networks.existsById(networkId)) {
            throw new ResourceNotFoundException("Network", networkId);
        }
    }

    private static int clamp(int limit) {
        return Math.max(1, Math.min(limit, MAX_LIMIT));
    }
}
