package com.wifisense.service;

import com.wifisense.dto.MetricSummary;
import com.wifisense.dto.NetworkReport;
import com.wifisense.model.Measurement;
import com.wifisense.model.Network;
import com.wifisense.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/** Basic aggregated reports and side-by-side comparison of networks. */
@Service
@Transactional(readOnly = true)
public class ReportService {

    private static final Duration MAX_RANGE = Duration.ofDays(31);

    private final NetworkRepository networks;
    private final MeasurementRepository measurements;
    private final AlertRepository alerts;
    private final AnalysisResultRepository analyses;
    private final AiPredictionRepository predictions;
    private final Clock clock;

    public ReportService(NetworkRepository networks, MeasurementRepository measurements, AlertRepository alerts,
                         AnalysisResultRepository analyses, AiPredictionRepository predictions, Clock clock) {
        this.networks = networks;
        this.measurements = measurements;
        this.alerts = alerts;
        this.analyses = analyses;
        this.predictions = predictions;
        this.clock = clock;
    }

    public NetworkReport report(Long networkId, Instant from, Instant to) {
        Instant end = to == null ? Instant.now(clock) : to;
        Instant start = from == null ? end.minus(Duration.ofHours(24)) : from;
        if (start.isAfter(end) || Duration.between(start, end).compareTo(MAX_RANGE) > 0) {
            throw new IllegalArgumentException("Report range must be positive and at most 31 days");
        }
        Network network = networks.findById(networkId)
                .orElseThrow(() -> new ResourceNotFoundException("Network", networkId));
        List<Measurement> data = measurements.findByNetworkIdAndMeasuredAtBetweenOrderByMeasuredAtAsc(networkId, start, end);

        return new NetworkReport(networkId, network.getSsid(), start, end, data.size(),
                summarize(data, Measurement::getLatencyMs),
                summarize(data, Measurement::getJitterMs),
                summarize(data, Measurement::getPacketLossPct),
                summarize(data, Measurement::getBandwidthMbps),
                summarize(data, Measurement::getSignalStrengthDbm),
                alerts.countByNetworkIdAndCreatedAtBetween(networkId, start, end),
                analyses.countByNetworkIdAndCreatedAtBetween(networkId, start, end),
                predictions.countByNetworkIdAndAnomalyDetectedTrueAndCreatedAtBetween(networkId, start, end),
                data.stream().anyMatch(Measurement::isSimulated));
    }

    /** Duplicated ids are ignored; order of first appearance is kept. */
    public List<NetworkReport> compare(List<Long> networkIds, Instant from, Instant to) {
        Set<Long> unique = new LinkedHashSet<>(networkIds);
        if (unique.size() < 2 || unique.size() > 6) {
            throw new IllegalArgumentException("Compare between 2 and 6 different networks");
        }
        return unique.stream().map(id -> report(id, from, to)).toList();
    }

    private static MetricSummary summarize(List<Measurement> data, Function<Measurement, ? extends Number> metric) {
        return MetricSummary.of(data.stream().map(metric).toList());
    }
}
